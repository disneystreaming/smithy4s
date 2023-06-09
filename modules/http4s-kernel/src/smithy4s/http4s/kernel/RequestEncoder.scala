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

import org.http4s.Request
import smithy4s.kinds._

trait RequestEncoder[F[_], A] { self =>
  def addToRequest(request: Request[F], a: A): Request[F]
  def mapRequest(f: Request[F] => Request[F]): RequestEncoder[F, A] =
    new RequestEncoder[F, A] {
      def addToRequest(request: Request[F], a: A): Request[F] = f(
        self.addToRequest(request, a)
      )
    }
}

object RequestEncoder {

  def empty[F[_], A]: RequestEncoder[F, A] = new RequestEncoder[F, A] {
    def addToRequest(request: Request[F], a: A): Request[F] = request
  }

  def mapRequestK[F[_]](
      f: Request[F] => Request[F]
  ): PolyFunction[RequestEncoder[F, *], RequestEncoder[F, *]] =
    new PolyFunction[RequestEncoder[F, *], RequestEncoder[F, *]] {
      def apply[A](fa: RequestEncoder[F, A]): RequestEncoder[F, A] =
        new RequestEncoder[F, A] {
          def addToRequest(request: Request[F], a: A): Request[F] = f(
            fa.addToRequest(request, a)
          )
        }
    }

  def combine[F[_], A](
      left: RequestEncoder[F, A],
      right: RequestEncoder[F, A]
  ): RequestEncoder[F, A] = new RequestEncoder[F, A] {
    def addToRequest(request: Request[F], a: A): Request[F] =
      right.addToRequest(left.addToRequest(request, a), a)
  }

}
