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
package http

import smithy4s.kinds._
import smithy4s.server.UnaryServerCodecs
import smithy4s.capability.MonadThrowLike
import scala.annotation.nowarn
import smithy4s.http.HttpMethod

object HttpUnaryServerRouter {

  def apply[Alg[_[_, _, _, _, _]], F[_], Request, Response](
      service: smithy4s.Service[Alg]
  )(
      impl: service.Impl[F],
      makeServerCodecs: UnaryServerCodecs.Make[F, Request, Response],
      endpointMiddleware: Endpoint.Middleware[Request => F[Response]],
      getMethod: Request => HttpMethod,
      getPathSegments: Request => IndexedSeq[String],
      addDecodedPathParams: (Request, PathParams) => Request
  )(implicit F: MonadThrowLike[F]): Request => Option[F[Response]] = {
    val router =
      new HttpUnaryServerRouter[Alg, service.Operation, F, Request, Response](
        service,
        service.toPolyFunction[smithy4s.kinds.Kind1[F]#toKind5](impl),
        makeServerCodecs,
        endpointMiddleware,
        getMethod,
        getPathSegments,
        addDecodedPathParams
      )
    router.route(_)
  }

}

// scalafmt: {maxColumn = 120}
private class HttpUnaryServerRouter[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[_], Request, Response](
    service: smithy4s.Service.Aux[Alg, Op],
    impl: FunctorInterpreter[Op, F],
    makeServerCodecs: UnaryServerCodecs.Make[F, Request, Response],
    endpointMiddleware: Endpoint.Middleware[Request => F[Response]],
    getMethod: Request => HttpMethod,
    getPathSegments: Request => IndexedSeq[String],
    addDecodedPathParams: (Request, PathParams) => Request
)(implicit F: MonadThrowLike[F]) {

  @nowarn
  private final case class HttpEndpointHandler(
      httpEndpoint: HttpEndpoint[_],
      handler: Request => F[Response]
  )

  def route(request: Request): Option[F[Response]] = {
    val method = getMethod(request)
    val pathSegments = getPathSegments(request)
    perMethodEndpoint.get(method) match {
      case Some(httpUnaryEndpoints) =>
        val maybeMatched =
          httpUnaryEndpoints.iterator
            .map(ep => (ep.handler, ep.httpEndpoint.matches(pathSegments)))
            .find(_._2.isDefined)
        maybeMatched.flatMap {
          case (handler, Some(pathParams)) =>
            val amendedRequest = addDecodedPathParams(request, pathParams)
            Some(handler(amendedRequest))
          case (_, None) => None
        }

      case None => None
    }

  }

  private def makeHttpEndpointHandler[I, E, O, SI, SO](
      endpoint: service.Endpoint[I, E, O, SI, SO]
  ): Either[HttpEndpoint.HttpEndpointError, HttpEndpointHandler] = {
    HttpEndpoint.cast(endpoint.schema).map { httpEndpoint =>
      val handler = smithy4s.server.UnaryServerEndpoint(
        impl,
        endpoint,
        makeServerCodecs(endpoint.schema),
        endpointMiddleware.prepare(service)(endpoint)
      )
      HttpEndpointHandler(httpEndpoint, handler)
    }
  }

  private val httpEndpointHandlers: List[HttpEndpointHandler] =
    service.endpoints.toList
      .map { makeHttpEndpointHandler(_) }
      .collect { case Right(endpointWrapper) => endpointWrapper }

  private val perMethodEndpoint: Map[HttpMethod, List[HttpEndpointHandler]] =
    httpEndpointHandlers.groupBy(_.httpEndpoint.method)

}
