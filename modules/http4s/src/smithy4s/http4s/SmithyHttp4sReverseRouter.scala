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

import org.http4s._
import org.http4s.client.Client
import smithy4s.http4s.kernel._
import smithy4s.http4s.internals.SmithyHttp4sClientEndpoint
import smithy4s.kinds.Kind5
import smithy4s.kinds.PolyFunction5
import cats.effect.Concurrent
import smithy4s.kinds.Kind1

// scalafmt: { align.preset = most, danglingParentheses.preset = false, maxColumn = 240, align.tokens = [{code = ":"}]}

class SmithyHttp4sReverseRouter[Alg[_[_, _, _, _, _]], F[_]](
    baseUri:         Uri,
    val service:     smithy4s.Service[Alg],
    client:          Client[F],
    compilerContext: UnaryClientCodecs[F],
    middleware:      ClientEndpointMiddleware[F]
)(implicit effect:   Concurrent[F]) {
// format: on

  type ClientEndpoint[I, E, O, SI, SO] = I => F[O]
  val handler = new PolyFunction5[service.Endpoint, ClientEndpoint] {
    def apply[I, E, O, SI, SO](endpoint: service.Endpoint[I, E, O, SI, SO]): I => F[O] =
      SmithyHttp4sClientEndpoint
        .make(
          baseUri,
          client,
          endpoint,
          compilerContext,
          middleware.prepare(service)(endpoint)
        )
        .left
        .map { e =>
          throw new Exception(
            s"Operation ${endpoint.name} is not bound to http semantics",
            e
          )
        }
        .merge
  }

  val impl: service.Impl[F] = service.fromPolyFunction[Kind1[F]#toKind5] {
    new service.FunctorInterpreter[F] {
      val cached = handler.unsafeCacheBy(service.endpoints.map(Kind5.existential(_)), identity)
      def apply[I, E, O, SI, SO](operation: service.Operation[I, E, O, SI, SO]): F[O] = {
        val (input, ep) = service.endpoint(operation)
        cached(ep).apply(input)
      }
    }
  }

}
