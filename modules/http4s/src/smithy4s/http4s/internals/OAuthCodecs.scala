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
package http4s
package internals

import cats.effect.Concurrent
import cats.syntax.all._
import cats.data.EitherT
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder
import smithy4s.codecs.PayloadPath
import smithy4s.http.HttpDiscriminator
import smithy4s.http.Metadata
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.http4s.SimpleProtocolCodecs
import smithy4s.json.Json
import smithy4s.kinds.PolyFunction
import smithy4s.schema.CachedSchemaCompiler

// TODO: Move out of smithy4s.
private[http4s] class OAuthCodecs(
    val maxArity: Int,
    val explicitDefaultsEncoding: Boolean
) extends SimpleProtocolCodecs {

  private val jsonHintMask = alloy.SimpleRestJson.protocol.hintMask

  private val jsonUnderlyingCodecs = Json.payloadCodecs
    .withJsoniterCodecCompiler(
      Json.jsoniter
        .withHintMask(jsonHintMask)
        .withMaxArity(maxArity)
        .withExplicitDefaultsEncoding(explicitDefaultsEncoding)
    )

  val jsonMediaType = HttpMediaType("application/json")
  def jsonEntityEncoders[F[_]] = jsonUnderlyingCodecs.mapK(
    EntityEncoders.fromPayloadCodecK[F](jsonMediaType)
  )

  def jsonEntityDecoders[F[_]: Concurrent]
      : CachedSchemaCompiler[EntityDecoder[F, *]] = jsonUnderlyingCodecs.mapK(
    EntityDecoders.fromPayloadCodecK[F](jsonMediaType)
  )

  // TODO: Check whether this is appropriate
  private val errorHeaders = List(
    smithy4s.http.errorTypeHeader,
    // Adding X-Amzn-Errortype as well to facilitate interop
    // with Amazon-issued code-generators.
    smithy4s.http.amazonErrorTypeHeader
  )

  def makeServerCodecs[F[_]: Concurrent]: UnaryServerCodecs.Make[F] = {
    val messageDecoderCompiler = urlFormRequestDecoderCompilers[F]
    val responseEncoderCompiler = ResponseEncoder.restSchemaCompiler[F](
      Metadata.Encoder,
      jsonEntityEncoders[F]
    )

    UnaryServerCodecs.make[F](
      input = messageDecoderCompiler,
      output = responseEncoderCompiler,
      error = responseEncoderCompiler,
      errorHeaders = errorHeaders
    )
  }

  def makeClientCodecs[F[_]: Concurrent]: UnaryClientCodecs.Make[F] = {
    val messageDecoderCompiler =
      ResponseDecoder.restSchemaCompiler[F](
        Metadata.Decoder,
        jsonEntityDecoders[F]
      )
    val messageEncoderCompiler = urlFormRequestEncoderCompilers[F]
    UnaryClientCodecs.Make[F](
      input = messageEncoderCompiler,
      output = messageDecoderCompiler,
      error = messageDecoderCompiler,
      response =>
        Concurrent[F].pure(
          HttpDiscriminator.fromMetadata(
            errorHeaders,
            getResponseMetadata(response)
          )
        )
    )
  }

  private def urlFormRequestDecoderCompilers[F[_]: Concurrent]
      : CachedSchemaCompiler[RequestDecoder[F, *]] =
    RequestDecoder.restSchemaCompiler[F](
      metadataDecoderCompiler = Metadata.AwsDecoder,
      entityDecoderCompiler = UrlForm
        .Decoder(
          ignoreXmlFlattened = false,
          capitalizeStructAndUnionMemberNames = false
        )
        .mapK(
          new PolyFunction[UrlForm.Decoder, EntityDecoder[F, *]] {
            def apply[A](fa: UrlForm.Decoder[A]): EntityDecoder[F, A] =
              urlFormEntityDecoder[F].flatMapR(urlForm =>
                EitherT.liftF(
                  fa.decode(urlForm)
                    .left
                    .map(urlFormDecodeError =>
                      smithy4s.http.HttpPayloadError(
                        urlFormDecodeError.path,
                        "",
                        urlFormDecodeError.message
                      )
                    )
                    .liftTo[F]
                )
              )
          }
        )
    )

  private def urlFormEntityDecoder[F[_]: Concurrent]
      : EntityDecoder[F, UrlForm] =
    EntityDecoders.fromHttpMediaReader(
      HttpMediaTyped(
        HttpMediaType("application/x-www-form-urlencoded"),
        blob =>
          UrlForm
            .parse(blob.toUTF8String)
            .left
            .map(urlFormDecodeError =>
              smithy4s.http.HttpPayloadError(
                PayloadPath.root,
                "",
                urlFormDecodeError.message
              )
            )
      )
    )

  private def urlFormRequestEncoderCompilers[F[_]: Concurrent]
      : CachedSchemaCompiler[RequestEncoder[F, *]] =
    RequestEncoder.restSchemaCompiler[F](
      metadataEncoderCompiler = Metadata.AwsEncoder,
      entityEncoderCompiler = UrlForm
        .Encoder(
          ignoreXmlFlattened = false,
          capitalizeStructAndUnionMemberNames = false
        )
        .mapK(
          new PolyFunction[UrlForm.Encoder, EntityEncoder[F, *]] {
            def apply[A](fa: UrlForm.Encoder[A]): EntityEncoder[F, A] =
              urlFormEntityEncoder[F].contramap(a =>
                UrlForm(
                  fa.encode(a).values
                )
              )
          }
        )
    )

  private def urlFormEntityEncoder[F[_]]: EntityEncoder[F, UrlForm] =
    EntityEncoders.fromHttpMediaWriter(
      HttpMediaTyped(
        HttpMediaType("application/x-www-form-urlencoded"),
        (_: Any, urlForm: UrlForm) => Blob(urlForm.render)
      )
    )

}
