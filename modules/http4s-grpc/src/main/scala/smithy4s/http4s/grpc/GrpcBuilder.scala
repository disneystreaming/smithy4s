package smithy4s
package http4s
package grpc

import smithy4s.ShapeTag
import smithy4s.kinds.FunctorAlgebra
import org.http4s.HttpRoutes
import org.http4s.client.Client
import org.http4s.Uri
import org.http4s.implicits._
import smithy4s.client.UnaryClientCompiler
import org.http4s.Response
import smithy4s.http4s.grpc.internals.SimpleGrpcCodecs
import smithy4s.interopcats._
import smithy4s.http4s.internals.Http4sToSmithy4sClient
import smithy4s.protobuf.internals.GrpcPayloadCodecCompilerImpl
import smithy4s.protobuf.Protobuf
import cats.effect._
import cats.syntax.all._
import cats.data.OptionT
import smithy4s.http.HttpUnaryServerRouter
import org.http4s.Request
import org.http4s.HttpApp
import smithy4s.http4s.kernel.{ toSmithy4sHttpUri, toSmithy4sHttpMethod }
import smithy4s.kinds.PolyFunction5

abstract class GrpcBuilder[P](protocolCodecs: SimpleProtocolCodecs)(implicit protocolTag: ShapeTag[P]) {
  def apply[Alg[_[_, _, _, _, _]]](
      service: smithy4s.Service[Alg]
  ): ServiceBuilder[Alg] = new ServiceBuilder(service)

  class ServiceBuilder[Alg[_[_, _, _, _, _]]]private[grpc](val service: smithy4s.Service[Alg]) {
    def routes[F[_]: Concurrent](
      impl: FunctorAlgebra[Alg, F]
    ): RouterBuilder[Alg, F] = new RouterBuilder[Alg, F](service, impl)

    def client[F[_]: Concurrent](client: Client[F]) =
      new ClientBuilder[Alg, F](client, service)
  }

  class RouterBuilder[
    Alg[_[_, _, _, _, _]],
    F[_]: Concurrent
  ] private[grpc] (
      service: smithy4s.Service[Alg],
      impl: FunctorAlgebra[Alg, F],
    ) {
    def make: Either[UnsupportedProtocolError, HttpRoutes[F]] = {
      checkProtocol(service, protocolTag).map{ _ =>
        val serviceUri = s"${service.id.namespace}.${service.id.name}"
        val finalMiddleware = Endpoint.Middleware.noop[HttpApp[F]]

        val mapper =
          new PolyFunction5[service.Endpoint, service.Endpoint] {
            def apply[I, E, O, SI, SO](
                endpoint: service.Endpoint[I, E, O, SI, SO]
            ): service.Endpoint[I, E, O, SI, SO] =
              endpoint
                .mapSchema {
                  _.withHints(
                    smithy.api.Http(smithy.api.NonEmptyString("POST"), smithy.api.NonEmptyString(s"${serviceUri}/${endpoint.id.name}"), 200)
                  )
                }
          }

        val router =
            HttpUnaryServerRouter(
              service = service.toBuilder.mapEndpointEach(mapper).build,
              encodeErrorsBeforeMiddleware = false,
              onError = PartialFunction.empty,
            )(
              impl,
              protocolCodecs.makeServerCodecs[F],
              finalMiddleware.biject(_.run)(HttpApp(_)),
              getMethod =
                (request: Request[F]) => toSmithy4sHttpMethod(request.method),
              getUri =
                (request: Request[F]) => toSmithy4sHttpUri(request.uri, None),
              addDecodedPathParams = (request: Request[F], _) => request
            )
        HttpRoutes(
          router.andThen(OptionT.fromOption(_).flatMap(OptionT.liftF(_)))
        )
      }
    }

    def resource: Resource[F, HttpRoutes[F]] =
      make.leftWiden[Throwable].liftTo[Resource[F, *]]
  }

  class ClientBuilder[
    Alg[_[_, _, _, _, _]],
    F[_]: Concurrent
  ] private[grpc](
    client: Client[F],
    val service: smithy4s.Service[Alg],
    baseUri: Uri = uri"http://localhost:8080",
  ) {

    def uri(uri: Uri): ClientBuilder[Alg, F] = new ClientBuilder[Alg, F](this.client, this.service, uri)

    def make: Either[UnsupportedProtocolError, service.Impl[F]] = {
      checkProtocol(service, protocolTag).map { _ =>
        val serviceUri = s"${service.id.namespace}.${service.id.name}"
        service.impl {
          UnaryClientCompiler(
            service,
            client,
            (client: Client[F]) => Http4sToSmithy4sClient(client),
            protocolCodecs.makeClientCodecs[F](baseUri / serviceUri),
            Endpoint.Middleware.noop,
            (response: Response[F]) => response.status.isSuccess
          )
        }
      }
    }

    def resource: Resource[F, service.Impl[F]] =
      make.leftWiden[Throwable].liftTo[Resource[F, *]]
  }
}

object GrpcBuilder extends GrpcBuilder[alloy.proto.Grpc](
  new SimpleGrpcCodecs(new GrpcPayloadCodecCompilerImpl(Protobuf.codecs))
  )
