/*
 *  Copyright 2023 Disney Streaming
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

package smithy4s
package aws
package internals

import cats.effect.Concurrent
import cats.syntax.all._
import fs2.compression.Compression
import smithy4s.Endpoint
import smithy4s.schema.CachedSchemaCompiler
import org.http4s.EntityDecoder
import smithy4s.xml.XmlDocument
import org.http4s.MediaRange
import fs2.data.xml._
import fs2.data.xml.dom._
import smithy4s.http._
import smithy4s.http4s.kernel._
import cats.data.EitherT
import smithy4s.kinds.PolyFunction
import smithy4s.codecs.PayloadPath
import org.http4s.EntityEncoder
import smithy4s.capability.Covariant
import _root_.aws.protocols.AwsQueryError

import smithy4s.http.Metadata

private[aws] object AwsQueryCodecs {

  def make[F[_]: Concurrent: Compression](
      version: String
  ): UnaryClientCodecs.Make[F] =
    new UnaryClientCodecs.Make[F] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {

        val stringAndBlobsEntityDecoders =
          smithy4s.http.StringAndBlobCodecs.ReaderCompiler
            .mapK(
              Covariant.liftPolyFunction[Option](
                EntityDecoders.fromHttpMediaReaderK[F]
              )
            )

        val urlFormEntityEncoders: CachedSchemaCompiler[EntityEncoder[F, *]] =
          UrlForm.Encoder
            .mapK(
              new PolyFunction[UrlForm.Encoder, EntityEncoder[F, *]] {
                def apply[A](fa: UrlForm.Encoder[A]): EntityEncoder[F, A] =
                  urlFormEntityEncoder[F].contramap((a: A) =>
                    UrlForm(
                      UrlForm.FormData.MultipleValues(
                        UrlForm.FormData
                          .PathedValue(
                            PayloadPath(PayloadPath.Segment("Action")),
                            maybeValue = Some(endpoint.id.name)
                          )
                          .toPathedValues ++
                          UrlForm.FormData
                            .PathedValue(
                              PayloadPath(PayloadPath.Segment("Version")),
                              maybeValue = Some(version)
                            )
                            .toPathedValues ++
                          fa.encode(a).values.values
                      )
                    )
                  )
              }
            )

        val xmlEntityDecoders: CachedSchemaCompiler[EntityDecoder[F, *]] =
          XmlDocument.Decoder.mapK(
            new PolyFunction[XmlDocument.Decoder, EntityDecoder[F, *]] {
              def apply[A](
                  fa: XmlDocument.Decoder[A]
              ): EntityDecoder[F, A] =
                xmlEntityDecoder[F].flatMapR(xmlDocument =>
                  EitherT.liftF {
                    fa.decode(xmlDocument)
                      .leftMap(fromXmlToHttpError)
                      .liftTo[F]
                  }
                )
            }
          )

        val decoders = CachedSchemaCompiler.getOrElse(
          stringAndBlobsEntityDecoders,
          xmlEntityDecoders
        )

        val restEncoders =
          RequestEncoder
            .restSchemaCompiler[F](
              Metadata.AwsEncoder,
              urlFormEntityEncoders,
              writeEmptyStructs = true
            )

        val restDecoders =
          ResponseDecoder.restSchemaCompiler(Metadata.AwsDecoder, decoders)

        val responseTag = endpoint.name + "Response"
        val resultTag = endpoint.name + "Result"
        val successDecoders = restDecoders.contramapSchema(
          smithy4s.schema.Schema.transformHintsLocallyK(
            _ ++ smithy4s.Hints(
              smithy4s.xml.internals.XmlStartingPath(
                List(responseTag, resultTag)
              )
            )
          )
        )

        val errorDecoders = restDecoders.contramapSchema(
          smithy4s.schema.Schema.transformHintsLocallyK(
            _ ++ smithy4s.Hints(
              smithy4s.xml.internals.XmlStartingPath(
                List("ErrorResponse", "Error")
              )
            )
          )
        )

        // Takes the `@awsQueryError` trait into consideration to decide
        // how to discriminate error responses.
        val errorNameMapping: String => String = {
          endpoint.errorable match {
            case None => identity[String]
            case Some(err) =>
              val mapping = err.error.alternatives.flatMap { alt =>
                val shapeName = alt.schema.shapeId.name
                alt.hints.get(AwsQueryError).map(_.code).map(_ -> shapeName)
              }.toMap
              (errorCode: String) => mapping.getOrElse(errorCode, errorCode)
          }
        }

        val discriminator = AwsErrorTypeDecoder
          .fromResponse(errorDecoders)
          .andThen(_.map(_.map {
            case HttpDiscriminator.NameOnly(name) =>
              HttpDiscriminator.NameOnly(errorNameMapping(name))
            case other => other
          }))

        val transformEncoders =
          applyCompression[F](endpoint.hints, retainUserEncoding = false)
        val finalEncoders = transformEncoders(restEncoders)

        val make =
          UnaryClientCodecs
            .Make[F](
              finalEncoders,
              successDecoders,
              errorDecoders,
              discriminator
            )
        make.apply(endpoint)
      }
    }

  private def xmlEntityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, XmlDocument] =
    EntityDecoder.decodeBy(
      MediaRange.parse("application/xml").getOrElse(sys.error("TODO"))
    )(media =>
      EitherT.liftF(
        media.body
          .through(fs2.text.utf8.decode[F])
          .through(events[F, String]())
          .through(referenceResolver())
          .through(normalize)
          .through(documents[F, XmlDocument])
          .head
          .compile
          .last
          .map(
            _.getOrElse(
              // TODO: This isn't right
              XmlDocument(
                XmlDocument.XmlElem(
                  XmlDocument.XmlQName(None, "Unit"),
                  List.empty,
                  List.empty
                )
              )
            )
          )
      )
    )

  private def urlFormEntityEncoder[F[_]: Concurrent]
      : EntityEncoder[F, UrlForm] = EntityEncoders.fromHttpMediaWriter(
    HttpMediaTyped(
      HttpMediaType("application/x-www-form-urlencoded"),
      (_: Any, urlForm: UrlForm) => Blob(urlForm.values.render)
    )
  )

  private def fromXmlToHttpError(
      xmlDecodeError: smithy4s.xml.XmlDecodeError
  ): smithy4s.http.HttpContractError =
    smithy4s.http.HttpPayloadError(
      xmlDecodeError.path.toPayloadPath,
      "",
      xmlDecodeError.message
    )

}
