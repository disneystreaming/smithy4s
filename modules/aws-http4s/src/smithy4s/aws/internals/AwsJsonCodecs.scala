/*
 *  Copyright 2021-2023 Disney Streaming
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
import smithy4s.capability.MonadThrowLike
import smithy4s.http._
import smithy4s.json.Json

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

  private[aws] val jsonDecoders = jsonPayloadCodecs.decoders
  private[aws] val jsonWriters = jsonPayloadCodecs.encoders

  def make[F[_]: MonadThrowLike](
      contentType: String
  ): HttpUnaryClientCodecs.Builder[F, HttpRequest[Blob], HttpResponse[Blob]] = {
    HttpUnaryClientCodecs.builder
      .withBodyEncoders(jsonWriters)
      .withSuccessBodyDecoders(jsonDecoders)
      .withErrorBodyDecoders(jsonDecoders)
      .withErrorDiscriminator(AwsErrorTypeDecoder.fromResponse(jsonDecoders))
      .withRequestMediaType(contentType)
  }

}
