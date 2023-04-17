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

trait RequestEncoder[F[_], A] {
  def addToRequest(request: Request[F], a: A): Request[F]
}

object RequestEncoder {

  def empty[F[_], A]: RequestEncoder[F, A] = new RequestEncoder[F, A] {
    def addToRequest(request: Request[F], a: A): Request[F] = request
  }

  def combine[F[_], A](
      left: RequestEncoder[F, A],
      right: RequestEncoder[F, A]
  ): RequestEncoder[F, A] = new RequestEncoder[F, A] {
    def addToRequest(request: Request[F], a: A): Request[F] =
      right.addToRequest(left.addToRequest(request, a), a)
  }

}
