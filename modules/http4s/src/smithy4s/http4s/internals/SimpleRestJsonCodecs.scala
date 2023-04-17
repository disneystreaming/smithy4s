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

import smithy4s.http.HttpDiscriminator
import smithy4s.http4s.kernel._
import cats.effect.Concurrent

private[http4s] class SimpleRestJsonCodecs(maxArity: Int)
    extends SimpleProtocolCodecs {
  private val hintMask =
    alloy.SimpleRestJson.protocol.hintMask ++ HintMask(IntEnum)
  private val underlyingCodecs = smithy4s.http.json.codecs(hintMask, maxArity)

  def makeServerCodecs[F[_]: Concurrent]: UnaryServerCodecs[F] = {
    val messageDecoderCompiler =
      MessageDecoder.restSchemaCompiler[F](
        EntityDecoders.fromCodecAPI[F](underlyingCodecs)
      )
    val messageEncoderCompiler =
      MessageEncoder.restSchemaCompiler[F](
        EntityEncoders.fromCodecAPI[F](underlyingCodecs)
      )
    UnaryServerCodecs.make[F](
      input = messageDecoderCompiler,
      output = messageEncoderCompiler,
      error = messageEncoderCompiler
    )
  }

  def makeClientCodecs[F[_]: Concurrent]: UnaryClientCodecs[F] = {
    val messageDecoderCompiler =
      MessageDecoder.restSchemaCompiler[F](
        EntityDecoders.fromCodecAPI[F](underlyingCodecs)
      )
    val messageEncoderCompiler =
      MessageEncoder.restSchemaCompiler[F](
        EntityEncoders.fromCodecAPI[F](underlyingCodecs)
      )
    UnaryClientCodecs.make[F](
      input = messageEncoderCompiler,
      output = messageDecoderCompiler,
      error = messageDecoderCompiler,
      response =>
        Concurrent[F].pure(
          HttpDiscriminator.fromMetadata(
            smithy4s.errorTypeHeader,
            getResponseMetadata(response)
          )
        )
    )
  }

}
