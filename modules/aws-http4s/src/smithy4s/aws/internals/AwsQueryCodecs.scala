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

package smithy4s
package aws
package internals

import cats.effect.Concurrent
import cats.syntax.all._
import fs2.compression.Compression
import smithy4s.Endpoint
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.http.Metadata
import smithy4s.kinds.PolyFunction
import smithy4s.codecs.PayloadPath
import org.http4s.EntityEncoder
import _root_.aws.protocols.AwsQueryError

private[aws] object AwsQueryCodecs {

  def make[F[_]: Concurrent: Compression](
      version: String
  ): UnaryClientCodecs.Make[F] =
    new UnaryClientCodecs.Make[F] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {
        val transformEncoders =
          applyCompression[F](endpoint.hints, retainUserEncoding = false)
        val requestEncoderCompilersWithCompression = transformEncoders(
          requestEncoderCompilers[F](
            action = endpoint.id.name,
            version = version
          )
        )

        val responseTag = endpoint.name + "Response"
        val resultTag = endpoint.name + "Result"
        val responseDecoderCompilers =
          AwsXmlCodecs
            .responseDecoderCompilers[F]
            .contramapSchema(
              smithy4s.schema.Schema.transformHintsLocallyK(
                _ ++ smithy4s.Hints(
                  smithy4s.xml.internals.XmlStartingPath(
                    List(responseTag, resultTag)
                  )
                )
              )
            )
        val errorDecoderCompilers = AwsXmlCodecs
          .responseDecoderCompilers[F]
          .contramapSchema(
            smithy4s.schema.Schema.transformHintsLocallyK(
              _ ++ smithy4s.Hints(
                smithy4s.xml.internals.XmlStartingPath(
                  List("ErrorResponse", "Error")
                )
              )
            )
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
            (errorCode: String) => mapping.getOrElse(errorCode, errorCode)
        }
        val errorDiscriminator = AwsErrorTypeDecoder
          .fromResponse(errorDecoderCompilers)
          .andThen(_.map(_.map {
            case HttpDiscriminator.NameOnly(name) =>
              HttpDiscriminator.NameOnly(errorNameMapping(name))
            case other => other
          }))

        val make = UnaryClientCodecs.Make[F](
          input = requestEncoderCompilersWithCompression,
          output = responseDecoderCompilers,
          error = errorDecoderCompilers,
          errorDiscriminator = errorDiscriminator
        )
        make.apply(endpoint)
      }
    }

  private def requestEncoderCompilers[F[_]: Concurrent](
      action: String,
      version: String
  ): CachedSchemaCompiler[RequestEncoder[F, *]] = {
    val urlFormEntityEncoderCompilers = UrlForm
      .Encoder()
      .mapK(
        new PolyFunction[UrlForm.Encoder, EntityEncoder[F, *]] {
          def apply[A](fa: UrlForm.Encoder[A]): EntityEncoder[F, A] =
            urlFormEntityEncoder[F].contramap((a: A) =>
              UrlForm(
                formData = UrlForm.FormData.MultipleValues(
                  values = Vector(
                    UrlForm.FormData.PathedValue(PayloadPath("Action"), action),
                    UrlForm.FormData
                      .PathedValue(PayloadPath("Version"), version)
                  ) ++ fa.encode(a).formData.values
                )
              )
            )
        }
      )
    RequestEncoder.restSchemaCompiler[F](
      metadataEncoderCompiler = Metadata.AwsEncoder,
      entityEncoderCompiler = urlFormEntityEncoderCompilers,
      writeEmptyStructs = true
    )
  }

  private def urlFormEntityEncoder[F[_]]: EntityEncoder[F, UrlForm] =
    EntityEncoders.fromHttpMediaWriter(
      HttpMediaTyped(
        HttpMediaType("application/x-www-form-urlencoded"),
        (_: Any, urlForm: UrlForm) => Blob(urlForm.render)
      )
    )

}
