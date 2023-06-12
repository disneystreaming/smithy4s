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
import smithy4s.http4s.kernel.UnaryClientCodecs.Make
import smithy4s.Endpoint

object CompressingClientCodecs {
  def apply[F[_]: Concurrent](
      compression: RequestEncoder.MiddlewareK[F],
      underlying: Make[F]
  ): Make[F] = new Make[F] {
    def apply[I, E, O, SI, SO](
        endpoint: Endpoint.Base[I, E, O, SI, SO]
    ): UnaryClientCodecs[F, I, E, O] = new UnaryClientCodecs[F, I, E, O] {
      val inner = underlying.apply(endpoint)
      val inputEncoder: RequestEncoder[F, I] = {
        endpoint.hints.get[smithy.api.RequestCompression] match {
          case Some(_) => compression(inner.inputEncoder)
          case None    => inner.inputEncoder
        }
      }
      val outputDecoder: ResponseDecoder[F, O] = inner.outputDecoder
      val errorDecoder: ResponseDecoder[F, Throwable] = inner.errorDecoder
    }
  }
}
