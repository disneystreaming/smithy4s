/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.aws
package internals

import _root_.aws.protocols.AwsQueryError
import alloy.UrlFormFlattened
import alloy.UrlFormName
import cats.effect.Concurrent
import smithy4s.interopcats._
import cats.syntax.all._
import fs2.compression.Compression
import org.http4s.Entity
import smithy.api.XmlFlattened
import smithy.api.XmlName
import smithy4s._
import smithy4s.codecs.PayloadPath
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.codecs.Writer
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.xml.internals.XmlStartingPath

// scalafmt: { maxColumn = 120}
private[aws] object AwsQueryCodecs {

  def make[F[_]: Concurrent: Compression](version: String): HttpUnaryClientCodecs.Make[F, Entity[F]] =
    new HttpUnaryClientCodecs.Make[F, Entity[F]] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): HttpUnaryClientCodecs[F, Entity[F], I, E, O] = {
        val addCompression = applyCompression[F](
          endpoint.hints,
          // To fulfil the requirement of
          // https://github.com/smithy-lang/smithy/blob/main/smithy-aws-protocol-tests/model/awsQuery/requestCompression.smithy#L152-L298.
          retainUserEncoding = false
        )
        val requestWriters = requestWriterCompiler[F](
          ignoreUrlFormFlattened = false,
          capitalizeStructAndUnionMemberNames = false,
          action = endpoint.id.name,
          version = version
        )
        val finalRequestWriters = addCompression(requestWriters)

        val responseTag = endpoint.name + "Response"
        val resultTag = endpoint.name + "Result"
        val responseReaders = xmlResponseReaders[F].contramapSchema(
          Schema.transformHintsLocallyK(_.addTargetHints(XmlStartingPath(List(responseTag, resultTag))))
        )
        val errorResponseReaders = xmlResponseReaders[F].contramapSchema(
          Schema.transformHintsLocallyK(_.addTargetHints(XmlStartingPath(List("ErrorResponse", "Error"))))
        )
        // Takes the `@awsQueryError` trait into consideration to decide how to
        // discriminate error responses.
        val errorNameMapping: (String => String) = endpoint.errorable match {
          case None =>
            identity[String]

          case Some(err) =>
            val mapping = err.error.alternatives.flatMap { alt =>
              val shapeName = alt.schema.shapeId.name
              alt.hints.get(AwsQueryError).map(_.code).map(_ -> shapeName)
            }.toMap
            errorCode => mapping.getOrElse(errorCode, errorCode)
        }
        val errorDiscriminator = AwsErrorTypeDecoder
          .fromResponse(errorResponseReaders)
          .andThen(_.map {
            case HttpDiscriminator.NameOnly(name) =>
              HttpDiscriminator.NameOnly(errorNameMapping(name))
            case other =>
              other
          })

        val make = HttpUnaryClientCodecs.Make[F, Entity[F]](
          finalRequestWriters,
          responseReaders,
          errorResponseReaders,
          errorDiscriminator,
          toStrict
        )
        make.apply(endpoint)
      }
    }

  private[internals] def requestWriterCompiler[F[_]: Concurrent](
      ignoreUrlFormFlattened: Boolean,
      capitalizeStructAndUnionMemberNames: Boolean,
      action: String,
      version: String
  ): CachedSchemaCompiler[HttpRequest.Encoder[Entity[F], *]] = {

    def urlFormToBlob(urlForm: UrlForm): Blob = Blob {
      UrlForm(
        UrlForm.FormData(PayloadPath("Action"), Some(action)) ::
          UrlForm.FormData(PayloadPath("Version"), Some(version)) ::
          urlForm.values
      ).render
    }

    val writers = UrlForm
      .Encoder(ignoreUrlFormFlattened, capitalizeStructAndUnionMemberNames)
      .mapK {
        UrlForm.Encoder.toWriterK
          .andThen(Writer.addingTo[Any].andThenK(urlFormToBlob))
          .andThen(EntityWriter.fromPayloadWriterK[F])
          .andThen(HttpRequest.Encoder.fromBodyEncoderK("application/x-www-form-urlencoded"))
      }
    HttpRequest.Encoder
      .restSchemaCompiler[Entity[F]](
        Metadata.AwsEncoder,
        writers,
        // We have to set this so that a body is produced even in the case where
        // a top-level struct input is empty. If it wasn't then the contramap
        // above wouldn't have the required effect because there would be no
        // UrlForm to add Action and Version to (literally no UrlForm value -
        // not just an empty one).
        writeEmptyStructs = true
      )
      .contramapSchema(
        // The AWS protocol works in terms of XmlFlattened and XmlName hints,
        // even for the input side, which is most definitely _not_ XML. Partly
        // because that seems odd, but mostly so that the URL form support can
        // be completely agnostic of AWS protocol details, they work with their
        // own more appropriately named hints - which is what necessitates the
        // translation here.
        Schema.transformHintsTransitivelyK { hints =>
          def translateFlattened(hints: Hints): Hints =
            hints.memberHints.get(XmlFlattened) match {
              case Some(_) => hints.addMemberHints(UrlFormFlattened())
              case None    => hints
            }
          def translateName(hints: Hints): Hints =
            hints.memberHints.get(XmlName) match {
              case Some(XmlName(name)) =>
                hints.addMemberHints(UrlFormName(name))
              case None => hints
            }
          (translateFlattened _ andThen translateName _)(hints)
        }
      )
  }

}
