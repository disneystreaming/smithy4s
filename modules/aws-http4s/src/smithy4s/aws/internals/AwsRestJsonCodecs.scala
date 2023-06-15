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
import fs2.compression.Compression
import smithy4s.aws.json.AwsSchemaVisitorJCodec
import smithy4s.http4s.kernel._
import smithy4s.http.HttpMediaType
import smithy4s.http.Metadata
import smithy4s.http.json.JCodec
import smithy4s.kinds.FunctorK
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.Endpoint

private[aws] object AwsRestJsonCodecs {

  private val hintMask = aws.protocols.RestJson1.protocol.hintMask

  def make[F[_]: Concurrent: Compression](
      contentType: String
  ): UnaryClientCodecs.Make[F] = {
    val httpMediaType = HttpMediaType(contentType)
    val underlyingCodecs = smithy4s.http.CodecAPI.nativeStringsAndBlob(
      new smithy4s.http.json.JsonCodecAPI(
        cache => new AwsSchemaVisitorJCodec(cache),
        Some(hintMask)
      ) {
        override def mediaType[A](codec: JCodec[A]): HttpMediaType.Type =
          httpMediaType
      }
    )

    val encoders = RequestEncoder.restSchemaCompiler[F](
      smithy4s.http.Metadata.Encoder,
      EntityEncoders.fromCodecAPI[F](underlyingCodecs)
    )

    val decoders = ResponseDecoder.restSchemaCompiler[F](
      Metadata.Decoder,
      EntityDecoders.fromCodecAPI[F](underlyingCodecs)
    )
    val discriminator = AwsErrorTypeDecoder.fromResponse(decoders)

    new UnaryClientCodecs.Make[F] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint.Base[I, E, O, SI, SO]
      ): UnaryClientCodecs[F, I, E, O] = {
        import smithy4s.capability.Encoder
        val compression = smithy4s.http4s.kernel.GzipRequestCompression[F]()
        val maybeCompressingEncoders =
          endpoint.hints.get(smithy.api.RequestCompression) match {
            case Some(rc) if rc.encodings.contains("gzip") =>
              FunctorK[CachedSchemaCompiler].mapK(
                encoders,
                Encoder.andThenK(compression)
              )
            case _ => encoders
          }
        val make = UnaryClientCodecs
          .Make[F](maybeCompressingEncoders, decoders, decoders, discriminator)
        make.apply(endpoint)
      }
    }

  }

}
