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
import cats.effect.{IO, Ref, Resource}
import fs2.{Chunk, Stream}
import org.http4s.{Header, Headers, HttpApp, HttpVersion, Method, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString
import smithy4s.{Blob, Endpoint, Hints, Schema, ShapeId}
import smithy4s.grpc._
import smithy4s.grpc.http4s.internals.{GrpcHttp4sCodecs, GrpcHttp4sLowLevelClient}
import smithy4s.grpc.internals.GrpcConstants
import smithy4s.kinds.{FunctorAlgebra, PolyFunction5}
import weaver.SimpleIOSuite

object GrpcProtocolBuilderSpec extends SimpleIOSuite {

  private val pingSchema =
    Schema.operation(ShapeId("test", "Ping"))

  private val pingEndpoint: Endpoint[DummyOp, Unit, Nothing, Unit, Nothing, Nothing] =
    new Endpoint[DummyOp, Unit, Nothing, Unit, Nothing, Nothing] {
      val schema = pingSchema
      def wrap(input: Unit): DummyOp[Unit, Nothing, Unit, Nothing, Nothing] = DummyOp.Ping
    }

  private object DummyService extends smithy4s.Service.Reflective[DummyOp] {
    val id: ShapeId                                                = ShapeId("test", "DummyService")
    val version: String                                            = ""
    val hints: Hints                                               = Hints(Grpc())
    val endpoints: IndexedSeq[Endpoint[_, _, _, _, _]]             = Vector(pingEndpoint)
    def input[I, E, O, SI, SO](op: DummyOp[I, E, O, SI, SO]): I   = op.input
    def ordinal[I, E, O, SI, SO](op: DummyOp[I, E, O, SI, SO]): Int = op.ordinal
  }

  private sealed trait DummyOp[I, E, O, SI, SO] {
    def input: I
    def ordinal: Int
    def endpoint: Endpoint[DummyOp, I, E, O, SI, SO]
  }

  private object DummyOp {
    case object Ping extends DummyOp[Unit, Nothing, Unit, Nothing, Nothing] {
      val input: Unit  = ()
      val ordinal: Int = 0
      val endpoint = pingEndpoint
    }
  }

  private val dummyImpl: FunctorAlgebra[PolyFunction5.From[DummyOp]#Algebra, IO] =
    new PolyFunction5[DummyOp, smithy4s.kinds.Kind1[IO]#toKind5] {
      def apply[I, E, O, SI, SO](op: DummyOp[I, E, O, SI, SO]): IO[O] =
        IO.pure(().asInstanceOf[O])
    }

  private val grpcBody: Stream[IO, Byte] =
    Stream.chunk(Chunk.array(GrpcFrame.encode(GrpcFrame(compressed = false, Blob.empty))))

  test("requireHttp2 returns 426 for HTTP/1.1 request") {
    GrpcProtocolBuilder.withRequireHttp2(true)(DummyService).routes(dummyImpl).make.fold(
      err => IO.pure(failure(err.getMessage)),
      routes => {
        val request = Request[IO](
          method = Method.POST,
          uri = Uri.unsafeFromString("/test.DummyService/Ping"),
          httpVersion = HttpVersion.`HTTP/1.1`
        ).withContentType(`Content-Type`(GrpcHttp4sCodecs.grpcProtoMediaType))
        routes.orNotFound(request).map { response =>
          expect.eql(response.status, Status.UpgradeRequired)
        }
      }
    )
  }

  test("GET returns 405 with Allow header") {
    GrpcProtocolBuilder.withRequireHttp2(false)(DummyService).routes(dummyImpl).make.fold(
      err => IO.pure(failure(err.getMessage)),
      routes => {
        val request = Request[IO](
          method = Method.GET,
          uri = Uri.unsafeFromString("/test.DummyService/Ping")
        ).withContentType(`Content-Type`(GrpcHttp4sCodecs.grpcProtoMediaType))
        routes.orNotFound(request).map { response =>
          expect.eql(response.status, Status.MethodNotAllowed) &&
          expect(response.headers.headers.exists(h => h.name.toString == "Allow" && h.value == "POST"))
        }
      }
    )
  }

  test("unsupported content-type returns 415") {
    GrpcProtocolBuilder.withRequireHttp2(false)(DummyService).routes(dummyImpl).make.fold(
      err => IO.pure(failure(err.getMessage)),
      routes => {
        val request = Request[IO](
          method = Method.POST,
          uri = Uri.unsafeFromString("/test.DummyService/Ping")
        ).withContentType(`Content-Type`(org.http4s.MediaType.text.plain))
        routes.orNotFound(request).map { response =>
          expect.eql(response.status, Status.UnsupportedMediaType)
        }
      }
    )
  }

  test("unknown gRPC method returns UNIMPLEMENTED in trailers") {
    GrpcProtocolBuilder.withRequireHttp2(false)(DummyService).routes(dummyImpl).make.fold(
      err => IO.pure(failure(err.getMessage)),
      routes => {
        val request = Request[IO](
          method = Method.POST,
          uri = Uri.unsafeFromString("/test.DummyService/Unknown")
        ).withContentType(`Content-Type`(GrpcHttp4sCodecs.grpcProtoMediaType))
          .withBodyStream(grpcBody)
        routes.orNotFound(request).flatMap { response =>
          response.trailerHeaders.map { trailers =>
            val status  = trailers.get(CIString(GrpcConstants.grpcStatusHeader)).map(_.head.value)
            val message = trailers.get(CIString(GrpcConstants.grpcMessageHeader)).map(_.head.value)
            expect.eql(response.status, Status.Ok) &&
            expect.same(status, Some(GrpcStatusCode.UNIMPLEMENTED.intValue.toString)) &&
            expect.same(message, Some(GrpcMessageEncoding.encode("Method not found: /test.DummyService/Unknown")))
          }
        }
      }
    )
  }

  test("trailing slash on gRPC path is accepted") {
    GrpcProtocolBuilder.withRequireHttp2(false)(DummyService).routes(dummyImpl).make.fold(
      err => IO.pure(failure(err.getMessage)),
      routes => {
        val request = Request[IO](
          method = Method.POST,
          uri = Uri.unsafeFromString("/test.DummyService/Ping/")
        ).withContentType(`Content-Type`(GrpcHttp4sCodecs.grpcProtoMediaType))
          .withBodyStream(grpcBody)
        routes.orNotFound(request).flatMap { response =>
          response.trailerHeaders.map { trailers =>
            val status = trailers.get(CIString(GrpcConstants.grpcStatusHeader)).map(_.head.value)
            expect.eql(response.status, Status.Ok) &&
            expect.same(status, Some(GrpcStatusCode.OK.intValue.toString))
          }
        }
      }
    )
  }

  test("server maps unknown throwable to INTERNAL") {
    val failingImpl: FunctorAlgebra[PolyFunction5.From[DummyOp]#Algebra, IO] =
      new PolyFunction5[DummyOp, smithy4s.kinds.Kind1[IO]#toKind5] {
        def apply[I, E, O, SI, SO](op: DummyOp[I, E, O, SI, SO]): IO[O] =
          IO.raiseError(new RuntimeException("boom"))
      }
    GrpcProtocolBuilder.withRequireHttp2(false)(DummyService).routes(failingImpl).make.fold(
      err => IO.pure(failure(err.getMessage)),
      routes => {
        val request = Request[IO](
          method = Method.POST,
          uri = Uri.unsafeFromString("/test.DummyService/Ping")
        ).withContentType(`Content-Type`(GrpcHttp4sCodecs.grpcProtoMediaType))
          .withBodyStream(grpcBody)
        routes.orNotFound(request).flatMap { response =>
          response.trailerHeaders.map { trailers =>
            val status  = trailers.get(CIString(GrpcConstants.grpcStatusHeader)).map(_.head.value)
            val message = trailers.get(CIString(GrpcConstants.grpcMessageHeader)).map(_.head.value)
            expect.eql(response.status, Status.Ok) &&
            expect.same(status, Some(GrpcStatusCode.INTERNAL.intValue.toString)) &&
            expect.same(message, Some(GrpcMessageEncoding.encode("Unhandled gRPC error")))
          }
        }
      }
    )
  }

  test("server middleware receives service and endpoint ids") {
    for {
      ref <- Ref.of[IO, Option[(ShapeId, ShapeId)]](None)
      middleware = new Endpoint.Middleware.Standard[GrpcRequest[Blob] => IO[GrpcResponse[Blob]]] {
        def prepare(
            serviceId: ShapeId,
            endpointId: ShapeId,
            serviceHints: Hints,
            endpointHints: Hints
        ): (GrpcRequest[Blob] => IO[GrpcResponse[Blob]]) => (GrpcRequest[Blob] => IO[GrpcResponse[Blob]]) =
          handler => request => ref.set(Some(serviceId -> endpointId)) *> handler(request)
      }
      routes <- GrpcProtocolBuilder
        .withRequireHttp2(false)(DummyService)
        .routes(dummyImpl)
        .middleware(middleware)
        .make
        .fold(err => IO.raiseError(new RuntimeException(err.getMessage)), IO.pure)
      request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString("/test.DummyService/Ping")
      ).withContentType(`Content-Type`(GrpcHttp4sCodecs.grpcProtoMediaType))
        .withBodyStream(grpcBody)
      _        <- routes.orNotFound(request)
      captured <- ref.get
    } yield expect.same(captured, Some(DummyService.id -> pingEndpoint.id))
  }

  test("low-level client sends TE: trailers header") {
    for {
      ref <- Ref.of[IO, Option[Request[IO]]](None)
      client = Client[IO] { request =>
        Resource.eval(
          ref.set(Some(request)).as(Response[IO](status = Status.Ok).withTrailerHeaders(IO.pure(Headers.empty)))
        )
      }
      lowLevel = GrpcHttp4sLowLevelClient(client, Uri.unsafeFromString("http://localhost"), requireHttp2 = false)
      request = GrpcRequest(
        message = Blob.empty, headers = GrpcMetadata.empty, timeout = None,
        compression = GrpcCompression.Identity, acceptedCompressions = Nil,
        path = GrpcMethodPath("test.DummyService", "Ping")
      )
      _        <- lowLevel.run(request)(IO.pure)
      captured <- ref.get
    } yield {
      val hasTe = captured.exists(_.headers.headers.exists(h => h.name.toString == "TE" && h.value == "trailers"))
      expect(hasTe)
    }
  }

  test("low-level client propagates grpc-timeout header") {
    for {
      ref <- Ref.of[IO, Option[Request[IO]]](None)
      client = Client[IO] { request =>
        Resource.eval(
          ref.set(Some(request)).as(Response[IO](status = Status.Ok).withTrailerHeaders(IO.pure(Headers.empty)))
        )
      }
      lowLevel = GrpcHttp4sLowLevelClient(client, Uri.unsafeFromString("http://localhost"), requireHttp2 = false)
      timeout  = GrpcTimeout(10, GrpcTimeout.Seconds)
      request = GrpcRequest(
        message = Blob.empty, headers = GrpcMetadata.empty, timeout = Some(timeout),
        compression = GrpcCompression.Identity, acceptedCompressions = Nil,
        path = GrpcMethodPath("test.DummyService", "Ping")
      )
      _        <- lowLevel.run(request)(IO.pure)
      captured <- ref.get
    } yield {
      val hasTimeout = captured.exists(
        _.headers.headers.exists(h => h.name.toString == GrpcConstants.grpcTimeoutHeader && h.value == timeout.toHeader)
      )
      expect(hasTimeout)
    }
  }

  test("low-level client handles trailers-only responses") {
    val httpApp = HttpApp[IO] { _ =>
      val headers = Headers(
        `Content-Type`(GrpcHttp4sCodecs.grpcProtoMediaType),
        Header.Raw(CIString(GrpcConstants.grpcStatusHeader), "7"),
        Header.Raw(CIString(GrpcConstants.grpcMessageHeader), GrpcMessageEncoding.encode("denied")),
        Header.Raw(CIString("grpc-foo"), "bar"),
        Header.Raw(CIString(GrpcConstants.grpcEncodingHeader), "gzip")
      )
      IO.pure(Response[IO](status = Status.Ok, headers = headers))
    }
    val client   = Client.fromHttpApp(httpApp)
    val lowLevel = GrpcHttp4sLowLevelClient(client, Uri.unsafeFromString("http://localhost"), requireHttp2 = false)
    val request = GrpcRequest(
      message = Blob.empty, headers = GrpcMetadata.empty, timeout = None,
      compression = GrpcCompression.Identity, acceptedCompressions = Nil,
      path = GrpcMethodPath("test.DummyService", "Ping")
    )
    lowLevel.run(request)(IO.pure).map { response =>
      val status   = response.trailers.getText(GrpcConstants.grpcStatusHeader).headOption
      val custom   = response.trailers.getText("grpc-foo").headOption
      val encoding = response.trailers.getText(GrpcConstants.grpcEncodingHeader).headOption
      expect.same(status, Some("7")) &&
      expect.same(custom, Some("bar")) &&
      expect(encoding.isEmpty) &&
      expect(response.headers.getText(GrpcConstants.grpcStatusHeader).isEmpty)
    }
  }

  test("low-level client does not enforce strict trailers") {
    val httpApp = HttpApp[IO] { _ =>
      IO.pure(Response[IO](status = Status.Ok).withTrailerHeaders(IO.pure(Headers.empty)))
    }
    val client   = Client.fromHttpApp(httpApp)
    val lowLevel = GrpcHttp4sLowLevelClient(client, Uri.unsafeFromString("http://localhost"), requireHttp2 = false)
    val request = GrpcRequest(
      message = Blob.empty, headers = GrpcMetadata.empty, timeout = None,
      compression = GrpcCompression.Identity, acceptedCompressions = Nil,
      path = GrpcMethodPath("test.DummyService", "Ping")
    )
    lowLevel.run(request)(IO.pure).map { response =>
      expect(response.trailers == GrpcMetadata.empty)
    }
  }

  test("low-level client enforces HTTP/2 when required") {
    val httpApp  = HttpApp[IO](_ => IO.pure(Response[IO](status = Status.Ok)))
    val client   = Client.fromHttpApp(httpApp)
    val lowLevel = GrpcHttp4sLowLevelClient(client, Uri.unsafeFromString("http://localhost"), requireHttp2 = true)
    val request = GrpcRequest(
      message = Blob.empty, headers = GrpcMetadata.empty, timeout = None,
      compression = GrpcCompression.Identity, acceptedCompressions = Nil,
      path = GrpcMethodPath("test.DummyService", "Ping")
    )
    lowLevel.run(request)(IO.pure).attempt.map { result =>
      expect(result == Left(GrpcFailure.Http2Required))
    }
  }

  test("toGrpcRequest limits body to max frame size") {
    val maxMessageSize     = 8
    val grpcHeaderSize     = 5L
    val overflowProbeBytes = 1L
    val limit              = grpcHeaderSize + maxMessageSize.toLong + overflowProbeBytes
    val body = Stream.chunk(Chunk.array(Array.fill(limit.toInt)(1.toByte))) ++
      Stream.raiseError[IO](new RuntimeException("pulled past limit"))
    val request = Request[IO](method = Method.POST)
      .withBodyStream(body)
      .withContentType(`Content-Type`(GrpcHttp4sCodecs.grpcProtoMediaType))
    GrpcHttp4sCodecs.toGrpcRequest(request, GrpcMethodPath("test.DummyService", "Ping"), maxMessageSize).map { grpcRequest =>
      expect.same(grpcRequest.message.size.toLong, limit)
    }
  }
}
