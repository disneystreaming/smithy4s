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
import smithy4s.http.CodecAPI
import org.http4s.implicits._
import smithy4s.http.json.JsonCodecAPI
import smithy4s.kinds._

/**
  * Abstract construct helping the construction of routers and clients
  * for a given protocol. Upon constructing the routers/clients, it will
  * first check that they are indeed annotated with the protocol in question.
  */
abstract class SimpleProtocolBuilder[P](val codecs: CodecAPI)(implicit
    protocolTag: ShapeTag[P]
) {

  def apply[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
      serviceProvider: smithy4s.Service.Provider[Alg, Op]
  ): ServiceBuilder[Alg, Op] = new ServiceBuilder(serviceProvider.service)

  def routes[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[_]](
      impl: FunctorAlgebra[Alg, F]
  )(implicit
      serviceProvider: smithy4s.Service.Provider[Alg, Op],
      F: EffectCompat[F]
  ): RouterBuilder[Alg, Op, F] = {
    val service = serviceProvider.service
    new RouterBuilder[Alg, Op, F](
      service,
      service.toPolyFunction[Kind1[F]#toKind5](impl),
      PartialFunction.empty
    )
  }

  class ServiceBuilder[
      Alg[_[_, _, _, _, _]],
      Op[_, _, _, _, _]
  ] private[http4s] (val service: smithy4s.Service[Alg, Op]) { self =>

    def client[F[_]: EffectCompat](client: Client[F]) =
      new ClientBuilder[Alg, Op, F](client, service)
    @deprecated(
      "Use the ClientBuilder instead,  client(client).uri(baseuri).use"
    )
    def client[F[_]: EffectCompat](
        http4sClient: Client[F],
        baseUri: Uri
    ): Either[UnsupportedProtocolError, FunctorAlgebra[Alg, F]] =
      client(http4sClient).uri(baseUri).use

    @deprecated(
      "Use the ClientBuilder instead , client(client).uri(baseuri).resource"
    )
    def clientResource[F[_]: EffectCompat](
        http4sClient: Client[F],
        baseUri: Uri
    ): Resource[F, FunctorAlgebra[Alg, F]] =
      client(http4sClient).uri(baseUri).resource

    def routes[F[_]: EffectCompat](
        impl: FunctorAlgebra[Alg, F]
    ): RouterBuilder[Alg, Op, F] =
      new RouterBuilder[Alg, Op, F](
        service,
        service.toPolyFunction[Kind1[F]#toKind5](impl),
        PartialFunction.empty
      )

  }

  class ClientBuilder[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _], F[
      _
  ]: EffectCompat] private[http4s] (
      client: Client[F],
      val service: smithy4s.Service[Alg, Op],
      uri: Uri = uri"http://localhost:8080",
      codecApi: CodecAPI = codecs
  ) {

    def uri(uri: Uri): ClientBuilder[Alg, Op, F] =
      new ClientBuilder[Alg, Op, F](this.client, this.service, uri)

    def withJsonCodec(codecAPI: JsonCodecAPI): ClientBuilder[Alg, Op, F] =
      new ClientBuilder[Alg, Op, F](
        this.client,
        this.service,
        this.uri,
        codecAPI
      )

    def resource: Resource[F, FunctorAlgebra[Alg, F]] =
      use.leftWiden[Throwable].liftTo[Resource[F, *]]

    def use: Either[UnsupportedProtocolError, FunctorAlgebra[Alg, F]] = {
      checkProtocol(service, protocolTag)
        .as(
          new SmithyHttp4sReverseRouter[Alg, Op, F](
            uri,
            service,
            client,
            EntityCompiler
              .fromCodecAPI[F](codecApi)
          )
        )
        .map(service.fromPolyFunction[Kind1[F]#toKind5](_))
    }
  }

  class RouterBuilder[
      Alg[_[_, _, _, _, _]],
      Op[_, _, _, _, _],
      F[_]
  ] private[http4s] (
      service: smithy4s.Service[Alg, Op],
      impl: FunctorInterpreter[Op, F],
      errorTransformation: PartialFunction[Throwable, F[Throwable]]
  )(implicit F: EffectCompat[F]) {

    val entityCompiler =
      EntityCompiler.fromCodecAPI(codecs)

    def mapErrors(
        fe: PartialFunction[Throwable, Throwable]
    ): RouterBuilder[Alg, Op, F] =
      new RouterBuilder(service, impl, fe andThen (e => F.pure(e)))

    def flatMapErrors(
        fe: PartialFunction[Throwable, F[Throwable]]
    ): RouterBuilder[Alg, Op, F] =
      new RouterBuilder(service, impl, fe)

    def make: Either[UnsupportedProtocolError, HttpRoutes[F]] =
      checkProtocol(service, protocolTag).as {
        new SmithyHttp4sRouter[Alg, Op, F](
          service,
          impl,
          errorTransformation,
          entityCompiler
        ).routes
      }

    def resource: Resource[F, HttpRoutes[F]] =
      make.leftWiden[Throwable].liftTo[Resource[F, *]]

  }

}
