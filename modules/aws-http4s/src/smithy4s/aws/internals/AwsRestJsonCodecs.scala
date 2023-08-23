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

import smithy4s.Blob
import smithy4s.codecs._
import smithy4s.http._
import smithy4s.json.Json
import smithy4s.interopcats._
import cats.effect.Concurrent
import fs2.compression.Compression
import smithy4s.http4s.kernel._
import smithy4s.kinds.PolyFunctions
import smithy4s.Endpoint
import org.http4s.Entity
import smithy4s.schema.CachedSchemaCompiler

// scalafmt: {maxColumn = 120}
private[aws] object AwsRestJsonCodecs {

  private val hintMask = aws.protocols.RestJson1.protocol.hintMask

  def make[F[_]: Concurrent: Compression](
      contentType: String
  ): UnaryClientCodecs.Make[F] = {
    val mediaType = HttpMediaType(contentType)

    val jsonPayloadCodecs =
      Json.payloadCodecs.withJsoniterCodecCompiler(
        Json.jsoniter
          .withInfinitySupport(true)
          .withFlexibleCollectionsSupport(true)
          .withHintMask(hintMask)
      )

    def nullToEmptyObject(blob: Blob): Blob =
      if (blob.sameBytesAs(Json.NullBlob)) Json.EmptyObjectBlob else blob

    val jsonWriters = jsonPayloadCodecs.mapK {
      PayloadCodec.writerK
        .andThen[PayloadWriter](Writer.andThenK(nullToEmptyObject))
        .andThen[EntityWriter[F, *]](EntityWriter.fromPayloadWriterK)
        .andThen[HttpRequest.Encoder[Entity[F], *]](HttpRequest.Encoder.fromEntityEncoderK(mediaType.value))
    }

    val jsonReaders =
      jsonPayloadCodecs.mapK {
        PayloadCodec.readerK
          .andThen[HttpPayloadReader](
            Reader.liftPolyFunction(PolyFunctions.mapErrorK(HttpContractError.fromPayloadError))
          )
          .andThen(EntityReader.fromHttpPayloadReaderK[F])
      }

    val mediaWriters = CachedSchemaCompiler.getOrElse(stringAndBlobRequestWriters[F], jsonWriters)
    val mediaReaders = CachedSchemaCompiler.getOrElse(stringAndBlobEntityReaders[F], jsonReaders)

    val requestWriters = HttpRequest.Encoder.restSchemaCompiler[Entity[F]](
      Metadata.AwsEncoder,
      mediaWriters
    )

    val responseReaders = HttpResponse.Decoder.restSchemaCompiler[F, Entity[F]](
      Metadata.AwsDecoder,
      mediaReaders
    )

    val discriminator = AwsErrorTypeDecoder.fromResponse(responseReaders)

    new UnaryClientCodecs.Make[F] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {
        val addCompression = applyCompression[F](endpoint.hints)
        val finalRequestWriters = addCompression(requestWriters)
        val make = HttpUnaryClientCodecs.Make[F, Entity[F]](
          finalRequestWriters,
          responseReaders,
          responseReaders,
          discriminator,
          toStrict
        )
        make.apply(endpoint)
      }
    }

  }

}
