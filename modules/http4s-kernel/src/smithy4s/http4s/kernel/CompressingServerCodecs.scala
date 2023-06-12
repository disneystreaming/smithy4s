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

package smithy4s.http4s.kernel

import cats.effect.Concurrent
import smithy4s.http4s.kernel.UnaryServerCodecs.Make
import smithy4s.Endpoint
import smithy4s.Schema

object CompressingServerCodecs {
  def apply[F[_]: Concurrent](
      compression: RequestDecoder.MiddlewareK[F],
      underlying: Make[F]
  ): Make[F] = new Make[F] {
    def apply[I, E, O, SI, SO](
        endpoint: Endpoint.Base[I, E, O, SI, SO]
    ): UnaryServerCodecs[F, I, E, O] = new UnaryServerCodecs[F, I, E, O] {
      val inner = underlying.apply(endpoint)
      val inputDecoder: RequestDecoder[F, I] =
        endpoint.hints.get[smithy.api.RequestCompression] match {
          case Some(_) => compression(inner.inputDecoder)
          case None    => inner.inputDecoder
        }
      val outputEncoder: ResponseEncoder[F, O] = inner.outputEncoder
      val errorEncoder: ResponseEncoder[F, E] = inner.errorEncoder
      def errorEncoder[EE](schema: Schema[EE]): ResponseEncoder[F, EE] =
        inner.errorEncoder(schema)
    }
  }
}
