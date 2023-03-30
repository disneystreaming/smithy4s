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
import smithy4s.kinds._

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
      F: Async[F]
  ): RouterBuilder[Alg, F] = {
    new RouterBuilder[Alg, F](
      service,
      impl,
      PartialFunction.empty,
      ServerEndpointMiddleware.noop[F]
    )
  }

  class ServiceBuilder[
      Alg[_[_, _, _, _, _]]
  ] private[http4s] (val service: smithy4s.Service[Alg]) { self =>

    def client[F[_]: Async](client: Client[F]) =
      new ClientBuilder[Alg, F](client, service)

    def routes[F[_]: Async](
        impl: FunctorAlgebra[Alg, F]
    ): RouterBuilder[Alg, F] =
      new RouterBuilder[Alg, F](
        service,
        impl,
        PartialFunction.empty,
        ServerEndpointMiddleware.noop[F]
      )

  }

  class ClientBuilder[
      Alg[_[_, _, _, _, _]],
      F[_]: Async
  ] private[http4s] (
      client: Client[F],
      val service: smithy4s.Service[Alg],
      uri: Uri = uri"http://localhost:8080",
      middleware: ClientEndpointMiddleware[F] = ClientEndpointMiddleware.noop[F]
  ) {

    def uri(uri: Uri): ClientBuilder[Alg, F] =
      new ClientBuilder[Alg, F](this.client, this.service, uri, this.middleware)

    def middleware(
        mid: ClientEndpointMiddleware[F]
    ): ClientBuilder[Alg, F] =
      new ClientBuilder[Alg, F](this.client, this.service, this.uri, mid)

    def resource: Resource[F, service.Impl[F]] =
      use.leftWiden[Throwable].liftTo[Resource[F, *]]

    def use: Either[UnsupportedProtocolError, service.Impl[F]] = {
      checkProtocol(service, protocolTag)
        // Making sure the router is evaluated lazily, so that all the compilation inside it
        // doesn't happen in case of a missing protocol
        .map { _ =>
          new SmithyHttp4sReverseRouter[Alg, F](
            uri,
            service,
            client,
            simpleProtocolCodecs.makeClientCodecs[F],
            middleware
          ).impl
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
      F: Async[F]
  ) {

    def mapErrors(
        fe: PartialFunction[Throwable, Throwable]
    ): RouterBuilder[Alg, F] =
      new RouterBuilder(service, impl, fe andThen (e => F.pure(e)), middleware)

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
          new SmithyHttp4sRouter[Alg, service.Operation, F](
            service,
            service.toPolyFunction[Kind1[F]#toKind5](impl),
            errorTransformation,
            simpleProtocolCodecs.makeServerCodecs[F],
            middleware
          ).routes
        }

    def resource: Resource[F, HttpRoutes[F]] =
      make.leftWiden[Throwable].liftTo[Resource[F, *]]

  }

}
