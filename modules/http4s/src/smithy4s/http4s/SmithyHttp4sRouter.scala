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

import cats.data.Kleisli
import cats.data.OptionT
import cats.implicits._
import org.http4s._
import smithy4s.http4s.internals.SmithyHttp4sServerEndpoint

// format: off
class SmithyHttp4sRouter[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[_]](
    service: smithy4s.Service[Alg, Op],
    impl: Interpreter[Op, F],
    errorTransformation: PartialFunction[Throwable, F[Throwable]],
    entityCompiler: EntityCompiler[F]
)(implicit effect: EffectCompat[F]) {

  private val compilerContext = internals.CompilerContext.make(entityCompiler)

  val routes: HttpRoutes[F] = Kleisli { request =>
    for {
      endpoints <- perMethodEndpoint.get(request.method).toOptionT[F]
      path = request.uri.path.segments.map(_.decoded()).toArray
      (endpoint, pathParams) <- endpoints.collectFirstSome(_.matchTap(path)).toOptionT[F]
      response <- OptionT.liftF(endpoint.run(pathParams, request))
    } yield response
  }
  // format: on

  private val http4sEndpoints: List[SmithyHttp4sServerEndpoint[F]] =
    service.endpoints
      .map { ep =>
        SmithyHttp4sServerEndpoint(
          impl,
          ep,
          compilerContext,
          errorTransformation
        )
      }
      .collect { case Some(http4sEndpoint) =>
        http4sEndpoint
      }

  private val perMethodEndpoint
      : Map[org.http4s.Method, List[SmithyHttp4sServerEndpoint[F]]] =
    http4sEndpoints.groupBy(_.method)

}
