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

import org.http4s.HttpApp

// format: off
trait ServerEndpointMiddleware[F[_]] {
  def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
      endpoint: Endpoint[service.Operation, _, _, _, _, _]
  ): HttpApp[F] => HttpApp[F]
}
// format: on

object ServerEndpointMiddleware {

  trait Simple[F[_]] extends ServerEndpointMiddleware[F] {
    def prepareWithHints(
        serviceHints: Hints,
        endpointHints: Hints
    ): HttpApp[F] => HttpApp[F]

    final def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
        endpoint: Endpoint[service.Operation, _, _, _, _, _]
    ): HttpApp[F] => HttpApp[F] =
      prepareWithHints(service.hints, endpoint.hints)
  }

  private[http4s] type EndpointMiddleware[F[_], Op[_, _, _, _, _]] =
    Endpoint[Op, _, _, _, _, _] => HttpApp[F] => HttpApp[F]

  def noop[F[_]]: ServerEndpointMiddleware[F] =
    new ServerEndpointMiddleware[F] {
      override def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
          endpoint: Endpoint[service.Operation, _, _, _, _, _]
      ): HttpApp[F] => HttpApp[F] = identity
    }

}
