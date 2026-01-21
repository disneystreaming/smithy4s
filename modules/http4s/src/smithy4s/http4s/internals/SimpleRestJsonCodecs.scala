/*
 *  Copyright 2021-2026 Disney Streaming
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
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import smithy4s.client._
import smithy4s.codecs.BlobEncoder
import smithy4s.http.HttpDiscriminator
import smithy4s.http.HttpMethod
import smithy4s.http.Metadata
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.interopcats._
import smithy4s.json.JsonPayloadCodecCompiler
import smithy4s.schema.FieldFilter

// scalafmt: {maxColumn = 120}
private[http4s] class SimpleRestJsonCodecs(
    val jsonCodecs: JsonPayloadCodecCompiler,
    val fieldFilter: FieldFilter,
    val hostPrefixInjection: Boolean,
    val smithyPathEncoding: Boolean
) extends SimpleProtocolCodecs {
  private val hintMask =
    alloy.SimpleRestJson.protocol.hintMask

  def transformJsonCodecs(f: JsonPayloadCodecCompiler => JsonPayloadCodecCompiler): SimpleRestJsonCodecs =
    new SimpleRestJsonCodecs(f(jsonCodecs), fieldFilter, hostPrefixInjection, smithyPathEncoding)

  @deprecated(
    message = """Use withFieldFilter instead.

  Mapping:
   - newExplicitDefaultsEncoding = false -> FieldFilter.Default
   - newExplicitDefaultsEncoding = true -> FieldFilter.EncodeAll
 """,
    since = "0.18.30"
  )
  protected def withExplicitDefaultEncoding(newExplicitDefaultsEncoding: Boolean): SimpleRestJsonCodecs =
    withFieldFilter(
      if (newExplicitDefaultsEncoding) FieldFilter.EncodeAll else FieldFilter.Default
    )

  @deprecated
  protected val explicitDefaultsEncoding: Boolean = fieldFilter == FieldFilter.EncodeAll

  def withFieldFilter(
      fieldFilter: FieldFilter
  ): SimpleRestJsonCodecs = new SimpleRestJsonCodecs(
    jsonCodecs.configureJsoniterCodecCompiler(_.withFieldFilter(fieldFilter)),
    fieldFilter,
    hostPrefixInjection,
    smithyPathEncoding
  )

  @deprecated("Use withSmithyPathEncoding instead (it has the opposite meaning to raw http label values)", "0.18.41")
  def withRawHttpLabelValues(enabled: Boolean): SimpleRestJsonCodecs =
    withSmithyPathEncoding(!enabled)

  def withSmithyPathEncoding(enabled: Boolean): SimpleRestJsonCodecs =
    new SimpleRestJsonCodecs(
      jsonCodecs = jsonCodecs,
      fieldFilter = fieldFilter,
      hostPrefixInjection = hostPrefixInjection,
      smithyPathEncoding = enabled
    )

  def withHostPrefixInjection(newHostPrefixInjection: Boolean): SimpleRestJsonCodecs =
    new SimpleRestJsonCodecs(jsonCodecs, fieldFilter, newHostPrefixInjection, smithyPathEncoding)

  // val mediaType = HttpMediaType("application/json")
  private val payloadEncoders: BlobEncoder.Compiler =
    jsonCodecs.configureJsoniterCodecCompiler(_.withHintMask(hintMask)).encoders

  private val payloadDecoders =
    jsonCodecs.configureJsoniterCodecCompiler(_.withHintMask(hintMask)).decoders

  // ALWAYS using maximum possible arity for client side, as this mechanism is a DDOS protection
  // that does not make sense for clients.
  private val clientPayloadDecoders =
    jsonCodecs.configureJsoniterCodecCompiler(_.withHintMask(hintMask).withMaxArity(Int.MaxValue)).decoders

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
      .withSuccessBodyDecoders(clientPayloadDecoders)
      .withErrorBodyDecoders(clientPayloadDecoders)
      .withErrorDiscriminator(HttpDiscriminator.fromResponse(errorHeaders, _).pure[F])
      .withMetadataDecoders(Metadata.Decoder)
      .withMetadataEncoders(
        Metadata.Encoder.withFieldFilter(fieldFilter)
      )
      .withBaseRequest(_ => baseRequest.pure[F])
      .withRequestMediaType("application/json")
      .withAcceptMediaType("application/json")
      .withRequestTransformation(fromSmithy4sHttpRequest[F](_, encodePathSegments = !smithyPathEncoding).pure[F])
      .withResponseTransformation[Response[F]](toSmithy4sHttpResponse[F](_))
      .withHostPrefixInjection(hostPrefixInjection)
      .withSmithyPathEncoding(smithyPathEncoding)
      .build()

  }

}
