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
package http4s.internals

import cats.effect.Concurrent
import org.http4s._
import org.http4s.client.Client
import smithy4s.http4s.ClientEndpointMiddleware
import smithy4s.client._
import smithy4s.interopcats._

// scalafmt: { align.preset = most, danglingParentheses.preset = false, maxColumn = 240, align.tokens = [{code = ":"}]}

private[http4s] object SmithyHttp4sReverseRouter {

  def impl[Alg[_[_, _, _, _, _]], F[_]](
      service:          smithy4s.Service[Alg],
      client:           Client[F],
      makeClientCodecs: UnaryClientCodecs.Make[F, Request[F], Response[F]],
      middleware:       ClientEndpointMiddleware[F]
  )(implicit effect:    Concurrent[F]): service.Impl[F] = service.impl {
    new service.FunctorEndpointCompiler[F] {
      def apply[I, E, O, SI, SO](endpoint: service.Endpoint[I, E, O, SI, SO]): I => F[O] = {

        val transformedClient = middleware.prepare(service)(endpoint).apply(client)

        val adaptedClient = Http4sToSmithy4sClient(transformedClient)
        val isSuccessful = (response: Response[F]) => response.status.isSuccess

        UnaryClientEndpoint(
          adaptedClient,
          makeClientCodecs(endpoint),
          isSuccessful
        )
      }
    }
  }

}
