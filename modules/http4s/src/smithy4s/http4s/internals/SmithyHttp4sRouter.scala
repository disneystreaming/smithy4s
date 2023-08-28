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

import cats.data.OptionT
import cats.effect.Concurrent
import cats.implicits._
import org.http4s._
import smithy4s.http4s.ServerEndpointMiddleware
import smithy4s.http4s.kernel._
import smithy4s.kinds._
import smithy4s.server.UnaryServerCodecs
import smithy4s.http.HttpEndpoint

// scalafmt: {maxColumn = 120}
private[http4s] class SmithyHttp4sRouter[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[_]](
    service: smithy4s.Service.Aux[Alg, Op],
    impl: FunctorInterpreter[Op, F],
    makeServerCodecs: UnaryServerCodecs.Make[F, Request[F], Response[F]],
    middleware: ServerEndpointMiddleware[F]
)(implicit effect: Concurrent[F]) {

  val routes: HttpRoutes[F] = HttpRoutes { request =>
    for {
      endpoints <- perMethodEndpoint.get(request.method).toOptionT[F]
      path = request.uri.path.segments.map(_.decoded()).toArray
      (endpoint, pathParams) <- endpoints.collectFirstSome(_.matchTap(path)).toOptionT[F]
      response <- OptionT.liftF(endpoint.httpApp(request.withAttribute(pathParamsKey, pathParams)))
    } yield response
  }

  private val http4sEndpoints: List[SmithyHttp4sServerEndpoint[F]] =
    service.endpoints.toList
      .map { makeServerEndpoint(_) }
      .collect { case Right(http4sEndpoint) =>
        http4sEndpoint
      }

  private def makeServerEndpoint[I, E, O, SI, SO](
      endpoint: service.Endpoint[I, E, O, SI, SO]
  ): Either[HttpEndpoint.HttpEndpointError, SmithyHttp4sServerEndpoint[F]] =
    SmithyHttp4sServerEndpoint.make(
      impl,
      endpoint,
      makeServerCodecs(endpoint),
      middleware.prepare(service)(endpoint)
    )

  private val perMethodEndpoint: Map[org.http4s.Method, List[SmithyHttp4sServerEndpoint[F]]] =
    http4sEndpoints.groupBy(_.method)

}
