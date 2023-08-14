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
import org.http4s.EntityDecoder
import org.http4s.Response
import org.http4s.headers.`Content-Type`
import org.http4s.syntax.all._
import smithy4s.http.HttpDiscriminator
import smithy4s.http.Metadata
import smithy4s.http._
import smithy4s.http4s.kernel._
import smithy4s.json.Json
import smithy4s.schema.CachedSchemaCompiler
import cats.syntax.all._
import cats.Eq
import smithy4s.kinds._

private[http4s] class SimpleRestJsonCodecs(
    val maxArity: Int,
    val explicitDefaultsEncoding: Boolean
) extends SimpleProtocolCodecs {
  private val hintMask =
    alloy.SimpleRestJson.protocol.hintMask

  private implicit val caseInsensitiveEq: Eq[CaseInsensitive] =
    Eq.fromUniversalEquals

  private val underlyingCodecs = Json.payloadCodecs
    .withJsoniterCodecCompiler(
      Json.jsoniter
        .withHintMask(hintMask)
        .withMaxArity(maxArity)
        .withExplicitDefaultsEncoding(explicitDefaultsEncoding)
    )

  val mediaType = HttpMediaType("application/json")
  def entityEncoders[F[_]] = underlyingCodecs.mapK {
    EntityEncoders.fromPayloadCodecK[F](mediaType)
  }

  // scalafmt: {maxColumn = 120}
  def entityDecoders[F[_]: Concurrent]: CachedSchemaCompiler[EntityDecoder[F, *]] = underlyingCodecs.mapK {
    EntityDecoders.fromPayloadCodecK[F](mediaType)
  }

  private val errorHeaders = List(
    smithy4s.http.errorTypeHeader,
    // Adding X-Amzn-Errortype as well to facilitate interop
    // with Amazon-issued code-generators.
    smithy4s.http.amazonErrorTypeHeader
  )

  private def addEmptyJsonToResponse[F[_]](
      response: Response[F]
  ): Response[F] = {
    response
      .withBodyStream(
        response.body
          .ifEmpty(fs2.Stream.chunk(fs2.Chunk.array("{}".getBytes())))
      )
      .withContentType(`Content-Type`(mediaType"application/json"))
  }

  private type EncoderFromHints[F[_], A] = Hints => ResponseEncoder[F, A]

  def makeServerCodecs[F[_]: Concurrent]: UnaryServerCodecs.Make[F] = {
    val messageDecoderCompiler =
      RequestDecoder.restSchemaCompiler[F](
        Metadata.Decoder,
        entityDecoders[F]
      )
    val restSchemaCompiler = ResponseEncoder.restSchemaCompiler[F](
      Metadata.Encoder,
      entityEncoders[F]
    )

    val responseEncoderCompiler = {
      new CachedSchemaCompiler[EncoderFromHints[F, *]] {
        type Cache = restSchemaCompiler.Cache
        def createCache(): Cache = restSchemaCompiler.createCache()
        def fromSchema[A](schema: Schema[A]) = fromSchema(schema, createCache())

        def fromSchema[A](schema: Schema[A], cache: Cache) = endpointHints =>
          if (
            schema.isUnit || endpointHints
              .get[smithy.api.Http]
              .exists(h => CaseInsensitive(h.method.value) === CaseInsensitive("HEAD"))
          ) {
            restSchemaCompiler.fromSchema(schema, cache)
          } else {
            restSchemaCompiler
              .fromSchema(schema, cache)
              .andThen(addEmptyJsonToResponse(_))
          }
      }
    }

    new UnaryServerCodecs.Make[F] {
      val input = messageDecoderCompiler
      val output = responseEncoderCompiler

      val error = responseEncoderCompiler.mapK[EncoderFromHints[F, *], ResponseEncoder[F, *]] {
        new PolyFunction[EncoderFromHints[F, *], ResponseEncoder[F, *]] {
          def apply[A](fromHints: EncoderFromHints[F, A]): ResponseEncoder[F, A] = fromHints(Hints.empty)
        }
      }

      val requestDecoderCache: input.Cache = input.createCache()
      val errorResponseEncoderCache: error.Cache = error.createCache()
      val responseEncoderCache: output.Cache = output.createCache()

      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryServerCodecs[F, I, E, O] = {
        new UnaryServerCodecs[F, I, E, O] {
          val inputDecoder: RequestDecoder[F, I] =
            input.fromSchema(endpoint.input, requestDecoderCache)
          val outputEncoder: ResponseEncoder[F, O] =
            output.fromSchema(endpoint.output, responseEncoderCache)(endpoint.hints)
          val errorEncoder: ResponseEncoder[F, E] =
            ResponseEncoder.forError(
              errorHeaders,
              endpoint.errorable,
              error
            )
          def errorEncoder[EE](schema: Schema[EE]): ResponseEncoder[F, EE] =
            error.fromSchema(schema, errorResponseEncoderCache)
        }
      }
    }
  }

  def makeClientCodecs[F[_]: Concurrent]: UnaryClientCodecs.Make[F] = {
    val messageDecoderCompiler =
      ResponseDecoder.restSchemaCompiler[F](
        Metadata.Decoder,
        entityDecoders[F]
      )
    val messageEncoderCompiler =
      RequestEncoder.restSchemaCompiler[F](
        Metadata.Encoder,
        entityEncoders[F]
      )
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

}
