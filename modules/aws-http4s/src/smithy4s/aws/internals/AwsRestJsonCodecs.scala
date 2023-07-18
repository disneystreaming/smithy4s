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
import smithy4s.Endpoint

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
    val jsonMediaReaders =
      jsonPayloadCodecs.mapK {
        PayloadCodec.readerK
          .andThen[HttpPayloadReader](
            Reader.liftPolyFunction(PolyFunctions.mapErrorK(HttpContractError.fromPayloadError))
          )
          .andThen[HttpMediaReader](HttpMediaTyped.mediaTypeK(mediaType))
      }

    def nullToEmptyObject(blob: Blob): Blob =
      if (blob.sameBytesAs(Blob("null"))) Blob("{}") else blob

    val jsonMediaWriters = jsonPayloadCodecs.mapK {
      PayloadCodec.writerK
        .andThen[PayloadWriter](Writer.andThenK(nullToEmptyObject))
        .andThen[HttpMediaWriter](HttpMediaTyped.mediaTypeK(mediaType))
    }

    val mediaReaders =
      smithy4s.http.StringAndBlobCodecs.readerOr(jsonMediaReaders)

    val mediaWriters =
      smithy4s.http.StringAndBlobCodecs.writerOr(jsonMediaWriters)

    val encoders = RequestEncoder.restSchemaCompiler[F](
      Metadata.AwsEncoder,
      mediaWriters.mapK(EntityEncoders.fromHttpMediaWriterK[F])
    )

    val decoders = ResponseDecoder.restSchemaCompiler[F](
      Metadata.AwsDecoder,
      mediaReaders.mapK(EntityDecoders.fromHttpMediaReaderK[F])
    )
    val discriminator = AwsErrorTypeDecoder.fromResponse(decoders)

    new UnaryClientCodecs.Make[F] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {
        val transformEncoders = applyCompression[F](endpoint.hints)
        val finalEncoders = transformEncoders(encoders)
        val make = UnaryClientCodecs
          .Make[F](finalEncoders, decoders, decoders, discriminator)
        make.apply(endpoint)
      }
    }

  }

}
