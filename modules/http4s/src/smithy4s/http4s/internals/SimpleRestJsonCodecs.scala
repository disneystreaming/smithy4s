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
import smithy4s.interopcats._
import smithy4s.http.HttpDiscriminator
import smithy4s.http.Metadata
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.json.Json
import smithy4s.client._
import smithy4s.codecs.BlobEncoder
import cats.syntax.all._
import org.http4s.Response
import org.http4s.Request
import org.http4s.Uri
import smithy4s.http.HttpMethod

// scalafmt: {maxColumn = 120}
private[http4s] class SimpleRestJsonCodecs(
    val maxArity: Int,
    val explicitDefaultsEncoding: Boolean
) extends SimpleProtocolCodecs {
  private val hintMask =
    alloy.SimpleRestJson.protocol.hintMask

  private val jsonCodecs = Json.payloadCodecs
    .withJsoniterCodecCompiler(
      Json.jsoniter
        .withHintMask(hintMask)
        .withMaxArity(maxArity)
        .withExplicitDefaultsEncoding(explicitDefaultsEncoding)
    )

  // val mediaType = HttpMediaType("application/json")
  private val payloadEncoders: BlobEncoder.Compiler =
    jsonCodecs.writers

  private val payloadDecoders =
    jsonCodecs.readers

  // Adding X-Amzn-Errortype as well to facilitate interop with Amazon-issued code-generators.
  private val errorHeaders = List(
    smithy4s.http.errorTypeHeader,
    smithy4s.http.amazonErrorTypeHeader
  )

  def makeServerCodecs[F[_]: Concurrent] = {
    val baseResponse = HttpResponse(200, Map.empty, Blob.empty)
    HttpUnaryServerCodecs.builder
      .withBodyDecoders(payloadDecoders)
      .withSuccessBodyEncoders(payloadEncoders)
      .withErrorBodyEncoders(payloadEncoders)
      .withErrorTypeHeaders(errorHeaders: _*)
      .withMetadataDecoders(Metadata.Decoder)
      .withMetadataEncoders(Metadata.Encoder)
      .withBaseResponse(_ => baseResponse.pure[F])
      .withResponseMediaType("application/json")
      .withWriteEmptyStructs(!_.isUnit)
      .withRequestTransformation[Request[F]](toSmithy4sHttpRequest[F](_))
      .withResponseTransformation(fromSmithy4sHttpResponse[F](_).pure[F])
      .build()
  }

  def makeClientCodecs[F[_]: Concurrent](
      uri: Uri
  ): UnaryClientCodecs.Make[F, Request[F], Response[F]] = {
    val baseRequest = HttpRequest(HttpMethod.POST, toSmithy4sHttpUri(uri, None), Map.empty, Blob.empty)
    HttpUnaryClientCodecs.builder
      .withBodyEncoders(payloadEncoders)
      .withSuccessBodyDecoders(payloadDecoders)
      .withErrorBodyDecoders(payloadDecoders)
      .withErrorDiscriminator(HttpDiscriminator.fromResponse(errorHeaders, _).pure[F])
      .withMetadataDecoders(Metadata.Decoder)
      .withMetadataEncoders(Metadata.Encoder)
      .withBaseRequest(_ => baseRequest.pure[F])
      .withRequestMediaType("application/json")
      .withRequestTransformation(fromSmithy4sHttpRequest[F](_).pure[F])
      .withResponseTransformation[Response[F]](toSmithy4sHttpResponse[F](_))
      .build()

  }

}
