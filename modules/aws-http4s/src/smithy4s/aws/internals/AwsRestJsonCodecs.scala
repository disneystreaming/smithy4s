/*
 *  Copyright 2021-2024 Disney Streaming
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

// scalafmt: {maxColumn = 120}
private[aws] object AwsRestJsonCodecs {

  private val hintMask = aws.protocols.RestJson1.protocol.hintMask

  def make[F[_]: MonadThrowLike](
      contentType: String
  ): HttpUnaryClientCodecs.Builder[F, HttpRequest[Blob], HttpResponse[Blob]] = {

    val jsonPayloadCodecs =
      Json.payloadCodecs.withJsoniterCodecCompiler(
        Json.jsoniter
          .withInfinitySupport(true)
          .withFlexibleCollectionsSupport(true)
          .withHintMask(hintMask)
      )

    def nullToEmptyObject(blob: Blob): Blob =
      if (blob.sameBytesAs(Json.NullBlob)) Json.EmptyObjectBlob else blob

    val jsonWriters = jsonPayloadCodecs.encoders.mapK { smithy4s.codecs.Encoder.andThenK(nullToEmptyObject) }
    val jsonDecoders = jsonPayloadCodecs.decoders

    HttpUnaryClientCodecs.builder
      .withBodyEncoders(jsonWriters)
      .withSuccessBodyDecoders(jsonDecoders)
      .withErrorBodyDecoders(jsonDecoders)
      .withErrorDiscriminator(AwsErrorTypeDecoder.fromResponse(jsonDecoders))
      .withMetadataDecoders(Metadata.AwsDecoder)
      .withMetadataEncoders(Metadata.AwsEncoder)
      .withRawStringsAndBlobsPayloads
      .withRequestMediaType(contentType)
  }

}
