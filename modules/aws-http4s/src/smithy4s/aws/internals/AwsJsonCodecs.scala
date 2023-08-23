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

import cats.effect.Concurrent
import smithy4s.http4s.kernel._
import smithy4s.interopcats._
import smithy4s.http._
import smithy4s.json.Json
import fs2.compression.Compression
import org.http4s.Entity
import smithy4s.Endpoint
import smithy4s.codecs.PayloadCodec

/**
 * An client codec for the AWS_JSON_1.0/AWS_JSON_1.1 protocol
 */
private[aws] object AwsJsonCodecs {

  private val hintMask =
    aws.protocols.AwsJson1_0.protocol.hintMask ++
      aws.protocols.AwsJson1_1.protocol.hintMask

  private[aws] val jsonPayloadCodecs =
    Json.payloadCodecs.withJsoniterCodecCompiler(
      Json.jsoniter
        .withInfinitySupport(true)
        .withFlexibleCollectionsSupport(true)
        .withHintMask(hintMask)
    )

  def make[F[_]: Concurrent: Compression](
      contentType: String
  ): HttpUnaryClientCodecs.Make[F, Entity[F]] = {
    val httpMediaType = HttpMediaType(contentType)
    val requestWriters =
      jsonPayloadCodecs.mapK(
        PayloadCodec.writerK
          .andThen(EntityWriter.fromPayloadWriterK[F])
          .andThen(HttpRequest.Encoder.fromBodyEncoderK(httpMediaType.value))
      )
    val responseReaders = jsonPayloadCodecs.mapK(
      PayloadCodec.readerK
        .andThen(EntityReader.fromPayloadReaderK[F])
        .andThen(HttpResponse.extractBody)
    )

    val discriminator = AwsErrorTypeDecoder.fromResponse(responseReaders)
    new HttpUnaryClientCodecs.Make[F, Entity[F]] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): HttpUnaryClientCodecs[F, Entity[F], I, E, O] = {
        val transformEncoders = applyCompression[F](endpoint.hints)
        val finalRequestWriters = transformEncoders(requestWriters)
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
