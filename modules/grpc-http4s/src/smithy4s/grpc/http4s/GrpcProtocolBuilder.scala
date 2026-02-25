/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.grpc.http4s

import alloy.proto.{Grpc, GrpcStatusCode}
import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import org.http4s._
import org.http4s.client.Client
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import smithy4s.{Blob, Endpoint, Service, UnsupportedProtocolError, checkProtocol}
import smithy4s.client.UnaryClientCompiler
import smithy4s.grpc._
import smithy4s.grpc.http4s.internals.{GrpcHttp4sCodecs, GrpcHttp4sLowLevelClient}
import smithy4s.grpc.internals.{GrpcConstants, GrpcUnaryClientCodecs, GrpcUnaryServerCodecs}
import smithy4s.interopcats._
import smithy4s.kinds.FunctorAlgebra
import smithy4s.protobuf.ProtobufCodec
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.server.UnaryServerEndpoint

class GrpcProtocolBuilder private (
    payloadCompiler: CachedSchemaCompiler[ProtobufCodec]
)(
    requireHttp2: Boolean = true,
    maxMessageSize: Int = 4 * 1024 * 1024
) {

  def withRequireHttp2(value: Boolean): GrpcProtocolBuilder =
    new GrpcProtocolBuilder(payloadCompiler)(value, maxMessageSize)

  def withMaxMessageSize(value: Int): GrpcProtocolBuilder =
    new GrpcProtocolBuilder(payloadCompiler)(requireHttp2, value)

  def withPayloadCompiler(
      value: CachedSchemaCompiler[ProtobufCodec]
  ): GrpcProtocolBuilder =
    new GrpcProtocolBuilder(value)(requireHttp2, maxMessageSize)

  def apply[Alg[_[_, _, _, _, _]]](
      service: Service[Alg]
  ): ServiceBuilder[Alg] = new ServiceBuilder(service)

  def routes[Alg[_[_, _, _, _, _]], F[_]](
      impl: FunctorAlgebra[Alg, F]
  )(implicit service: Service[Alg], F: Concurrent[F]): RouterBuilder[Alg, F] =
    apply(service).routes(impl)

  class ServiceBuilder[Alg[_[_, _, _, _, _]]] private[http4s] (
      val service: Service[Alg]
  ) {

    def client[F[_]: Concurrent](client: Client[F]): ClientBuilder[Alg, F] =
      new ClientBuilder[Alg, F](
        client = client,
        service = service,
        uri = uri"http://localhost:50051",
        middleware = Endpoint.Middleware.noop,
        strictTrailers = false,
        strictStatusDetails = false,
        requireHttp2 = GrpcProtocolBuilder.this.requireHttp2,
        maxMessageSize = GrpcProtocolBuilder.this.maxMessageSize,
        payloadCompiler = GrpcProtocolBuilder.this.payloadCompiler
      )

    def routes[F[_]: Concurrent](
        impl: FunctorAlgebra[Alg, F]
    ): RouterBuilder[Alg, F] =
      new RouterBuilder[Alg, F](
        service = service,
        impl = impl,
        middleware = Endpoint.Middleware.noop,
        requireHttp2 = GrpcProtocolBuilder.this.requireHttp2,
        maxMessageSize = GrpcProtocolBuilder.this.maxMessageSize,
        payloadCompiler = GrpcProtocolBuilder.this.payloadCompiler
      )
  }

  class ClientBuilder[Alg[_[_, _, _, _, _]], F[_]] private[http4s] (
      client: Client[F],
      val service: Service[Alg],
      uri: Uri,
      middleware: GrpcClientMiddleware[F],
      strictTrailers: Boolean,
      strictStatusDetails: Boolean,
      requireHttp2: Boolean,
      maxMessageSize: Int,
      payloadCompiler: CachedSchemaCompiler[ProtobufCodec]
  )(implicit F: Concurrent[F]) {

    def uri(uri: Uri): ClientBuilder[Alg, F] = copy(uri = uri)

    def middleware(mid: GrpcClientMiddleware[F]): ClientBuilder[Alg, F] =
      copy(middleware = mid)

    def strictTrailers(value: Boolean): ClientBuilder[Alg, F] =
      copy(strictTrailers = value)

    def strictStatusDetails(value: Boolean): ClientBuilder[Alg, F] =
      copy(strictStatusDetails = value)

    def make: Either[UnsupportedProtocolError, service.Impl[F]] =
      checkProtocol(service, Grpc).map { _ =>
          val compiler = UnaryClientCompiler.make(
            service,
            client,
            (c: Client[F]) => GrpcHttp4sLowLevelClient(c, uri, requireHttp2),
            GrpcUnaryClientCodecs.builder(service)
              .withCompiler(payloadCompiler)
              .withMaxMessageSize(maxMessageSize)
              .withStrictTrailers(strictTrailers)
              .withStrictStatusDetails(strictStatusDetails)
              .build(),
            middleware,
            (response: GrpcResponse[Blob]) => F.pure(response.isSuccessful)
          )

        service.impl {
          new service.FunctorEndpointCompiler[F] {
            def apply[I, E, O, SI, SO](
                endpoint: service.Endpoint[I, E, O, SI, SO]
            ): I => F[O] =
              if (
                endpoint.schema.streamedInput.nonEmpty || endpoint.schema.streamedOutput.nonEmpty
              ) {
                _ =>
                  implicitly[Concurrent[F]].raiseError(
                    GrpcFailure.Unimplemented(
                      s"gRPC streaming not supported for ${service.id} ${endpoint.id}"
                    )
                  )
              } else {
                compiler(endpoint)
              }
          }
        }
      }

    def resource: Resource[F, service.Impl[F]] =
      make.leftWiden[Throwable].liftTo[Resource[F, *]]

    private def copy(
        client: Client[F] = client,
        service: Service[Alg] = service,
        uri: Uri = uri,
        middleware: GrpcClientMiddleware[F] = middleware,
        strictTrailers: Boolean = strictTrailers,
        strictStatusDetails: Boolean = strictStatusDetails,
        requireHttp2: Boolean = requireHttp2,
        maxMessageSize: Int = maxMessageSize,
        payloadCompiler: CachedSchemaCompiler[ProtobufCodec] = payloadCompiler
    ): ClientBuilder[Alg, F] =
      new ClientBuilder[Alg, F](
        client,
        service,
        uri,
        middleware,
        strictTrailers,
        strictStatusDetails,
        requireHttp2,
        maxMessageSize,
        payloadCompiler
      )
  }

  class RouterBuilder[Alg[_[_, _, _, _, _]], F[_]] private[http4s] (
      service: Service[Alg],
      impl: FunctorAlgebra[Alg, F],
      middleware: GrpcServerMiddleware[F],
      requireHttp2: Boolean,
      maxMessageSize: Int,
      payloadCompiler: CachedSchemaCompiler[ProtobufCodec]
  )(implicit F: Concurrent[F]) {

    def middleware(mid: GrpcServerMiddleware[F]): RouterBuilder[Alg, F] =
      copy(middleware = mid)

    def maxMessageSize(value: Int): RouterBuilder[Alg, F] =
      copy(maxMessageSize = value)

    def make: Either[UnsupportedProtocolError, HttpRoutes[F]] =
      checkProtocol(service, Grpc).map { _ =>
        val endpoints = service.endpoints
        val interpreter =
          service.toPolyFunction[smithy4s.kinds.Kind1[F]#toKind5](impl)
        val routes = endpoints.map { endpoint =>
          val path = GrpcProtocolBuilder.grpcPath(service)(endpoint)
          val handler =
            if (
              endpoint.schema.streamedInput.nonEmpty || endpoint.schema.streamedOutput.nonEmpty
            ) {
              (_: Request[F]) =>
                F.raiseError[Response[F]](
                  GrpcFailure.Unimplemented(
                    s"gRPC streaming not supported for ${service.id} ${endpoint.id}"
                  )
                )
            } else {
              endpointHandler(interpreter, endpoint)
            }
          path -> handler
        }.toMap

        HttpRoutes.of[F] { case request =>
          val isGrpcRequest =
            request.method == org.http4s.Method.POST &&
              request.headers
                .get[`Content-Type`]
                .exists(h => GrpcHttp4sCodecs.grpcContentTypes.contains(h.mediaType))
          val segments = request.uri.path.segments.map(_.decoded()).toList
          val parsedPath = smithy4s.grpc.GrpcMethodPath.parseSegments(
            segments,
            request.uri.path.endsWithSlash
          ).toOption
          parsedPath.flatMap(routes.get) match {
            case Some(handler) => handler(request)
            case None if isGrpcRequest =>
              val message = parsedPath match {
                case Some(valid) => s"Method not found: ${valid.render}"
                case None        => "Invalid gRPC method path"
              }
              val trailers = GrpcMetadata.empty
                .addText(GrpcConstants.grpcStatusHeader, GrpcStatusCode.UNIMPLEMENTED.intValue.toString)
                .addText(GrpcConstants.grpcMessageHeader, GrpcMessageEncoding.encode(message))
              GrpcHttp4sCodecs.grpcResponseToHttp4s[F](
                GrpcResponse(Blob.empty, GrpcMetadata.empty, trailers, GrpcCompression.Identity)
              )
            case None => Response.notFound[F].pure[F]
          }
        }
      }

    def resource: Resource[F, HttpRoutes[F]] =
      make.leftWiden[Throwable].liftTo[Resource[F, *]]

    private def endpointHandler[I, E, O, SI, SO](
        interpreter: smithy4s.kinds.FunctorInterpreter[service.Operation, F],
        endpoint: service.Endpoint[I, E, O, SI, SO]
    ): Request[F] => F[Response[F]] = {
          val base = UnaryServerEndpoint(
            interpreter,
            endpoint,
            GrpcUnaryServerCodecs.builder[F]
              .withCompiler(payloadCompiler)
              .withMaxMessageSize(maxMessageSize)
              .build()
              .apply(endpoint.schema),
            middleware.prepare(service)(endpoint)
          )

      val path = GrpcProtocolBuilder.grpcPath(service)(endpoint)

      (request: Request[F]) =>
        if (request.method != org.http4s.Method.POST) {
          Response[F](status = Status.MethodNotAllowed)
            .putHeaders(
              Header.Raw(org.typelevel.ci.CIString("Allow"), "POST")
            )
            .pure[F]
        } else if (
          requireHttp2 && request.httpVersion != HttpVersion.`HTTP/2`
        ) {
          Response[F](status = Status.UpgradeRequired).pure[F]
        } else if (
          !request.headers
            .get[`Content-Type`]
            .exists(h =>
              GrpcHttp4sCodecs.grpcContentTypes.contains(h.mediaType)
            )
        ) {
          Response[F](status = Status.UnsupportedMediaType).pure[F]
        } else {
          GrpcHttp4sCodecs.toGrpcRequest(request, path, maxMessageSize)
            .flatMap(base)
            .flatMap(GrpcHttp4sCodecs.grpcResponseToHttp4s[F])
        }
    }

    private def copy(
        service: Service[Alg] = service,
        impl: FunctorAlgebra[Alg, F] = impl,
        middleware: GrpcServerMiddleware[F] = middleware,
        requireHttp2: Boolean = requireHttp2,
        maxMessageSize: Int = maxMessageSize,
        payloadCompiler: CachedSchemaCompiler[ProtobufCodec] = payloadCompiler
    ): RouterBuilder[Alg, F] =
      new RouterBuilder[Alg, F](
        service,
        impl,
        middleware,
        requireHttp2,
        maxMessageSize,
        payloadCompiler
      )
  }
}

object GrpcProtocolBuilder {
  def grpcPath[Alg[_[_, _, _, _, _]]](
      service: Service[Alg]
  )(
      endpoint: Endpoint[service.Operation, _, _, _, _, _]
  ): smithy4s.grpc.GrpcMethodPath =
    smithy4s.grpc.GrpcMethodPath(
      s"${service.id.namespace}.${service.id.name}",
      endpoint.id.name
    )

  private[GrpcProtocolBuilder] val default: GrpcProtocolBuilder =
    new GrpcProtocolBuilder(ProtobufCodec)()

  def apply[Alg[_[_, _, _, _, _]]](
      service: Service[Alg]
  ): default.ServiceBuilder[Alg] = default(service)

  def routes[Alg[_[_, _, _, _, _]], F[_]](
      impl: FunctorAlgebra[Alg, F]
  )(implicit
      service: Service[Alg],
      F: Concurrent[F]
  ): default.RouterBuilder[Alg, F] =
    default.routes(impl)

  def withRequireHttp2(value: Boolean): GrpcProtocolBuilder =
    default.withRequireHttp2(value)

  def withMaxMessageSize(value: Int): GrpcProtocolBuilder =
    default.withMaxMessageSize(value)

  def withPayloadCompiler(
      value: CachedSchemaCompiler[ProtobufCodec]
  ): GrpcProtocolBuilder =
    default.withPayloadCompiler(value)

}
