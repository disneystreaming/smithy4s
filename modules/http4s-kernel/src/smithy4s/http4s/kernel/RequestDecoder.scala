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

import cats.~>
import org.http4s.Request

trait RequestDecoder[F[_], A] {
  def decodeRequest(request: Request[F]): F[A]
}
object RequestDecoder {
  type Middleware[F[_], A] = RequestDecoder[F, A] => RequestDecoder[F, A]
  type MiddlewareK[F[_]] = RequestDecoder[F, *] ~> RequestDecoder[F, *]

  def noop[F[_]]: RequestDecoder.MiddlewareK[F] =
    new (RequestDecoder[F, *] ~> RequestDecoder[F, *]) {
      def apply[A](
          fa: RequestDecoder[F, A]
      ): RequestDecoder[F, A] = fa
    }

  def combine[F[_]](
      a: RequestDecoder.MiddlewareK[F],
      b: RequestDecoder.MiddlewareK[F]
  ): RequestDecoder.MiddlewareK[F] =
    new (RequestDecoder[F, *] ~> RequestDecoder[F, *]) {
      def apply[A](
          fa: RequestDecoder[F, A]
      ): RequestDecoder[F, A] = a(b(fa))
    }
}
