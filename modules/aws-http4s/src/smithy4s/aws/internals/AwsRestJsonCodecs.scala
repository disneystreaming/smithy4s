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
import cats.effect.Concurrent
import fs2.compression.Compression
import smithy4s.http4s.kernel._
import smithy4s.kinds.PolyFunctions
import smithy4s.capability.Covariant
import smithy4s.Endpoint
import org.http4s.Entity
import smithy4s.schema.CachedSchemaCompiler

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

    // scalafmt: {maxColumn = 120}
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

    val stringAndBlobWriters = smithy4s.http.StringAndBlobCodecs.WriterCompiler.mapK {
      Covariant.liftPolyFunction[Option](
        HttpMediaTyped
          .liftPolyFunction(EntityWriter.fromPayloadWriterK[F])
          .andThen(HttpRequest.Encoder.fromHttpMediaWriterK)
      )
    }

    val stringAndBlobReaders: CachedSchemaCompiler.Optional[Reader[F, Entity[F], *]] =
      smithy4s.http.StringAndBlobCodecs.ReaderCompiler.mapK {
        Covariant.liftPolyFunction[Option](
          HttpMediaTyped
            .unwrappedK[HttpPayloadReader]
            .andThen(EntityReader.fromHttpPayloadReaderK[F])
        )
      }

    val mediaWriters = CachedSchemaCompiler.getOrElse(stringAndBlobWriters, jsonWriters)
    val mediaReaders = CachedSchemaCompiler.getOrElse(stringAndBlobReaders, jsonReaders)

    val encoders = HttpRequest.Encoder.restSchemaCompiler[Entity[F]](
      Metadata.AwsEncoder,
      mediaWriters
    )

    val decoders = HttpResponse.Decoder.restSchemaCompiler[F, Entity[F]](
      Metadata.AwsDecoder,
      mediaReaders
    )
    val discriminator = AwsErrorTypeDecoder.fromResponse(decoders)

    new UnaryClientCodecs.Make[F] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {
        val transformEncoders = applyCompression[F](endpoint.hints)
        val finalEncoders = transformEncoders(encoders)
        val make = HttpUnaryClientCodecs
          .Make[F, Entity[F]](finalEncoders, decoders, decoders, discriminator, toStrict)
        make.apply(endpoint)
      }
    }

  }

}
