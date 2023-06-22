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

import smithy4s.kinds._
import org.http4s._
import org.http4s.client.Client
import smithy4s.http4s.internals.SmithyHttp4sClientEndpoint

// format: off
class SmithyHttp4sReverseRouter[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[_]](
    baseUri: Uri,
    service: smithy4s.Service.Aux[Alg, Op],
    client: Client[F],
    entityCompiler: EntityCompiler[F],
    middleware: ClientEndpointMiddleware[F]
)(implicit effect: EffectCompat[F])
    extends FunctorInterpreter[Op, F] {
// format: on

  private val compilerContext = internals.CompilerContext.make(entityCompiler)

  def apply[I, E, O, SI, SO](
      op: Op[I, E, O, SI, SO]
  ): F[O] = {
    val (input, endpoint) = service.endpoint(op)
    val http4sEndpoint = clientEndpoints(endpoint)
    http4sEndpoint.send(input)
  }

  private val clientEndpoints =
    new PolyFunction5[
      Endpoint[Op, *, *, *, *, *],
      SmithyHttp4sClientEndpoint[F, Op, *, *, *, *, *]
    ] {
      def apply[I, E, O, SI, SO](
          endpoint: Endpoint[Op, I, E, O, SI, SO]
      ): SmithyHttp4sClientEndpoint[F, Op, I, E, O, SI, SO] =
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
              s"Operation ${endpoint.name} is not bound to http semantics: ${e.message}",
              e
            )
          }
          .merge
    }.unsafeCacheBy(
      service.endpoints.map(smithy4s.kinds.Kind5.existential(_)),
      identity
    )
}
