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

import smithy4s.HintMask
import smithy4s.IntEnum
import smithy4s.http4s.kernel._
import cats.effect.Concurrent

/**
 * An client codec for the AWS_JSON_1.0/AWS_JSON_1.1 protocol
 */
private[aws] object AwsJsonCodecs {

  private val hintMask =
    aws.protocols.AwsJson1_0.protocol.hintMask ++
      aws.protocols.AwsJson1_0.protocol.hintMask ++ HintMask(IntEnum)

  def make[F[_]: Concurrent]: UnaryClientCodecs[F] = {
    val underlyingCodecs = smithy4s.http.json.codecs(hintMask, 10000)
    val encoders = MessageEncoder.rpcSchemaCompiler[F](
      EntityEncoders.fromCodecAPI[F](underlyingCodecs)
    )
    val decoders = MessageDecoder.rpcSchemaCompiler[F](
      EntityDecoders.fromCodecAPI[F](underlyingCodecs)
    )
    val discriminator = AwsErrorTypeDecoder.fromResponse(decoders)
    UnaryClientCodecs.make[F](encoders, decoders, decoders, discriminator)
  }

}
