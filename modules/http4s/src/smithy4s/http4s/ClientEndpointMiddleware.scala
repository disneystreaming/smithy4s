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

import cats.Monoid
import org.http4s.client.Client
import org.http4s.HttpApp

// format: off
trait ClientEndpointMiddleware[F[_]] {
  def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
      endpoint: Endpoint[service.Operation, _, _, _, _, _]
  ): Client[F] => Client[F]
}
// format: on

object ClientEndpointMiddleware {

  trait Simple[F[_]] extends ClientEndpointMiddleware[F] {
    def prepareWithHints(
        serviceHints: Hints,
        endpointHints: Hints
    ): Client[F] => Client[F]

    final def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
        endpoint: Endpoint[service.Operation, _, _, _, _, _]
    ): Client[F] => Client[F] =
      prepareWithHints(service.hints, endpoint.hints)
  }

  def noop[F[_]]: ClientEndpointMiddleware[F] =
    new ClientEndpointMiddleware[F] {
      override def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
          endpoint: Endpoint[service.Operation, _, _, _, _, _]
      ): Client[F] => Client[F] = identity
    }

  implicit def monoidClientEndpointMiddleware[F[_]]
      : Monoid[ClientEndpointMiddleware[F]] =
    new Monoid[ClientEndpointMiddleware[F]] {
      def combine(
          a: ClientEndpointMiddleware[F],
          b: ClientEndpointMiddleware[F]
      ): ClientEndpointMiddleware[F] =
        new ClientEndpointMiddleware[F] {
          def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
              endpoint: Endpoint[service.Operation, _, _, _, _, _]
          ): HttpApp[F] => HttpApp[F] =
            a.prepare(service)(endpoint).andThen(b.prepare(service)(endpoint))
        }

      val empty: ClientEndpointMiddleware[F] =
        new ClientEndpointMiddleware[F] {
          def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
              endpoint: Endpoint[service.Operation, _, _, _, _, _]
          ): HttpApp[F] => HttpApp[F] =
            identity
        }
    }
}
