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
import smithy4s.http.HttpMediaType
import smithy4s.json.Json
import fs2.compression.Compression
import smithy4s.Endpoint

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
  ): UnaryClientCodecs.Make[F] = {
    val httpMediaType = HttpMediaType(contentType)
    val encoders = RequestEncoder.rpcSchemaCompiler[F](
      jsonPayloadCodecs.mapK(
        EntityEncoders.fromPayloadCodecK[F](httpMediaType)
      )
    )
    val decoders = jsonPayloadCodecs.mapK(
      EntityDecoders
        .fromPayloadCodecK[F](httpMediaType)
        .andThen(MediaDecoder.fromEntityDecoderK)
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
