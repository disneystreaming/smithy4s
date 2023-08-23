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
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.codecs.PayloadCodec
import org.http4s.Entity

// scalafmt: {maxColumn = 120}
private[http4s] class SimpleRestJsonCodecs(
    val maxArity: Int,
    val explicitDefaultsEncoding: Boolean
) extends SimpleProtocolCodecs {
  private val hintMask =
    alloy.SimpleRestJson.protocol.hintMask

  private val underlyingCodecs = Json.payloadCodecs
    .withJsoniterCodecCompiler(
      Json.jsoniter
        .withHintMask(hintMask)
        .withMaxArity(maxArity)
        .withExplicitDefaultsEncoding(explicitDefaultsEncoding)
    )

  // val mediaType = HttpMediaType("application/json")
  private def entityEncoders[F[_]]: CachedSchemaCompiler[EntityWriter[F, *]] =
    underlyingCodecs.mapK { PayloadCodec.writerK.andThen(EntityWriter.fromPayloadWriterK[F]) }

  private def entityDecoders[F[_]: Concurrent]: CachedSchemaCompiler[EntityReader[F, *]] =
    underlyingCodecs.mapK { PayloadCodec.readerK.andThen(EntityReader.fromPayloadReaderK) }

  private val errorHeaders = List(
    smithy4s.http.errorTypeHeader,
    // Adding X-Amzn-Errortype as well to facilitate interop
    // with Amazon-issued code-generators.
    smithy4s.http.amazonErrorTypeHeader
  )

  private def addEmptyJsonToResponse[F[_]](response: HttpResponse[Entity[F]]): HttpResponse[Entity[F]] = {
    val entity = response.body
    response
      .copy(body = entity.copy(body = entity.body.ifEmpty(fs2.Stream.chunk(fs2.Chunk.array("{}".getBytes())))))
      .withContentType("application/json")
  }

  def makeServerCodecs[F[_]: Concurrent]: HttpUnaryServerCodecs.Make[F, Entity[F]] = {
    val messageDecoderCompiler =
      HttpRequest.Decoder.restSchemaCompiler[F, Entity[F]](
        Metadata.Decoder,
        entityDecoders[F]
      )
    val responseEncoderCompiler = {
      val restSchemaCompiler = HttpResponse.Encoder.restSchemaCompiler[Entity[F]](
        Metadata.Encoder,
        entityEncoders[F],
        "application/json"
      )
      new CachedSchemaCompiler[HttpResponse.Encoder[Entity[F], *]] {
        type Cache = restSchemaCompiler.Cache
        def createCache(): Cache = restSchemaCompiler.createCache()
        def fromSchema[A](schema: Schema[A]) = fromSchema(schema, createCache())

        def fromSchema[A](schema: Schema[A], cache: Cache) = if (schema.isUnit) {
          restSchemaCompiler.fromSchema(schema, cache)
        } else {
          restSchemaCompiler
            .fromSchema(schema, cache)
            .andThen(addEmptyJsonToResponse(_))
        }
      }
    }

    smithy4s.http.HttpUnaryServerCodecs.Make[F, Entity[F]](
      input = messageDecoderCompiler,
      output = responseEncoderCompiler,
      error = responseEncoderCompiler,
      errorHeaders = errorHeaders
    )
  }

  def makeClientCodecs[F[_]: Concurrent]: HttpUnaryClientCodecs.Make[F, Entity[F]] = {
    val messageDecoderCompiler =
      HttpResponse.Decoder.restSchemaCompiler[F, Entity[F]](
        Metadata.Decoder,
        entityDecoders[F]
      )
    val messageEncoderCompiler =
      HttpRequest.Encoder.restSchemaCompiler[Entity[F]](
        Metadata.Encoder,
        entityEncoders[F],
        "application/json"
      )
    smithy4s.http.HttpUnaryClientCodecs.Make[F, Entity[F]](
      input = messageEncoderCompiler,
      output = messageDecoderCompiler,
      error = messageDecoderCompiler,
      response => Concurrent[F].pure(HttpDiscriminator.fromResponse(errorHeaders, response)),
      toStrict = smithy4s.http4s.kernel.toStrict[F]
    )
  }

}
