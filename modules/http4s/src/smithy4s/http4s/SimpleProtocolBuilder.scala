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

import cats.effect._
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.implicits._
import smithy4s.interopcats._
import smithy4s.kinds._
import smithy4s.client.UnaryClientCompiler
import smithy4s.http.HttpUnaryServerRouter
import smithy4s.http4s.internals.Http4sToSmithy4sClient
import smithy4s.http4s.kernel.{toSmithy4sHttpMethod, pathParamsKey}
import org.http4s.HttpApp
import org.http4s.Request
import org.http4s.Response
import cats.data.OptionT

/**
  * Abstract construct helping the construction of routers and clients
  * for a given protocol. Upon constructing the routers/clients, it will
  * first check that they are indeed annotated with the protocol in question.
  */
abstract class SimpleProtocolBuilder[P](
    simpleProtocolCodecs: SimpleProtocolCodecs
)(implicit
    protocolTag: ShapeTag[P]
) {

  def apply[Alg[_[_, _, _, _, _]]](
      service: smithy4s.Service[Alg]
  ): ServiceBuilder[Alg] = new ServiceBuilder(service)

  def routes[Alg[_[_, _, _, _, _]], F[_]](
      impl: FunctorAlgebra[Alg, F]
  )(implicit
      service: smithy4s.Service[Alg],
      F: Concurrent[F]
  ): RouterBuilder[Alg, F] = {
    new RouterBuilder[Alg, F](
      service,
      impl,
      PartialFunction.empty,
      Endpoint.Middleware.noop
    )
  }

  class ServiceBuilder[
      Alg[_[_, _, _, _, _]]
  ] private[http4s] (val service: smithy4s.Service[Alg]) { self =>

    def client[F[_]: Concurrent](client: Client[F]) =
      new ClientBuilder[Alg, F](client, service)

    def routes[F[_]: Concurrent](
        impl: FunctorAlgebra[Alg, F]
    ): RouterBuilder[Alg, F] =
      new RouterBuilder[Alg, F](
        service,
        impl,
        PartialFunction.empty,
        Endpoint.Middleware.noop
      )

  }

  class ClientBuilder[
      Alg[_[_, _, _, _, _]],
      F[_]: Concurrent
  ] private[http4s] (
      client: Client[F],
      val service: smithy4s.Service[Alg],
      uri: Uri = uri"http://localhost:8080",
      middleware: ClientEndpointMiddleware[F] =
        Endpoint.Middleware.noop[Client[F]]
  ) {

    def uri(uri: Uri): ClientBuilder[Alg, F] =
      new ClientBuilder[Alg, F](this.client, this.service, uri, this.middleware)

    def middleware(
        mid: ClientEndpointMiddleware[F]
    ): ClientBuilder[Alg, F] =
      new ClientBuilder[Alg, F](this.client, this.service, this.uri, mid)

    def resource: Resource[F, service.Impl[F]] =
      make.leftWiden[Throwable].liftTo[Resource[F, *]]

    def make: Either[UnsupportedProtocolError, service.Impl[F]] = {
      checkProtocol(service, protocolTag)
        // Making sure the router is evaluated lazily, so that all the compilation inside it
        // doesn't happen in case of a missing protocol
        .map { _ =>
          service.impl {
            UnaryClientCompiler(
              service,
              client,
              (client: Client[F]) => Http4sToSmithy4sClient(client),
              simpleProtocolCodecs.makeClientCodecs[F](uri),
              middleware,
              (response: Response[F]) => response.status.isSuccess
            )
          }
        }
    }
  }

  class RouterBuilder[
      Alg[_[_, _, _, _, _]],
      F[_]
  ] private[http4s] (
      service: smithy4s.Service[Alg],
      impl: FunctorAlgebra[Alg, F],
      errorTransformation: PartialFunction[Throwable, F[Throwable]],
      middleware: ServerEndpointMiddleware[F]
  )(implicit
      F: Concurrent[F]
  ) {

    /**
      * Applies the error transformation to the errors that are not in the smithy spec (has no effect on errors from spec).
      * Transformed errors raised in endpoint implementation will be observable from [[middleware]].
      * Errors raised in the [[middleware]] will be transformed too.
      *
      * The following two are equivalent:
      * {{{
      * val handlerPF: PartialFunction[Throwable, Throwable] = ???
      * builder.mapErrors(handlerPF).middleware(middleware)
      * }}}

      * {{{
      * val handlerPF: PartialFunction[Throwable, Throwable] = ???
      * val handler = ServerEndpointMiddleware.mapErrors(handlerPF)
      * builder.middleware(handler |+| middleware |+| handler)
      * }}}
      */
    def mapErrors(
        fe: PartialFunction[Throwable, Throwable]
    ): RouterBuilder[Alg, F] =
      new RouterBuilder(service, impl, fe andThen (e => F.pure(e)), middleware)

    /**
      * Applies the error transformation to the errors that are not in the smithy spec (has no effect on errors from spec).
      * Transformed errors raised in endpoint implementation will be observable from [[middleware]].
      * Errors raised in the [[middleware]] will be transformed too.
      *
      * The following two are equivalent:
      * {{{
      * val handlerPF: PartialFunction[Throwable, F[Throwable]] = ???
      * builder.flatMapErrors(handlerPF).middleware(middleware)
      * }}}

      * {{{
      * val handlerPF: PartialFunction[Throwable, F[Throwable]] = ???
      * val handler = ServerEndpointMiddleware.flatMapErrors(handlerPF)
      * builder.middleware(handler |+| middleware |+| handler)
      * }}}
      */
    def flatMapErrors(
        fe: PartialFunction[Throwable, F[Throwable]]
    ): RouterBuilder[Alg, F] =
      new RouterBuilder(service, impl, fe, middleware)

    def middleware(
        mid: ServerEndpointMiddleware[F]
    ): RouterBuilder[Alg, F] =
      new RouterBuilder[Alg, F](service, impl, errorTransformation, mid)

    def make: Either[UnsupportedProtocolError, HttpRoutes[F]] =
      checkProtocol(service, protocolTag)
        // Making sure the router is evaluated lazily, so that all the compilation inside it
        // doesn't happen in case of a missing protocol
        .map { _ =>
          val errorHandler =
            ServerEndpointMiddleware.flatMapErrors(errorTransformation)
          val finalMiddleware =
            errorHandler.andThen(middleware).andThen(errorHandler)
          val router = HttpUnaryServerRouter(service)(
            impl,
            simpleProtocolCodecs.makeServerCodecs[F],
            finalMiddleware.biject(_.run)(HttpApp(_)),
            getMethod =
              (request: Request[F]) => toSmithy4sHttpMethod(request.method),
            getPathSegments = (request: Request[F]) =>
              request.uri.path.segments.map(_.decoded()),
            addDecodedPathParams = (request: Request[F], pathParams) =>
              request.withAttribute(pathParamsKey, pathParams)
          )
          HttpRoutes(router.andThen(OptionT(_)))
        }

    def resource: Resource[F, HttpRoutes[F]] =
      make.leftWiden[Throwable].liftTo[Resource[F, *]]

  }

}
