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

package smithy4s.grpc

import alloy.proto.{Grpc, GrpcStatusCode}
import cats.effect.IO
import smithy4s.{Blob, Hints, Schema, ShapeId}
import smithy4s.grpc.internals.{GrpcConstants, GrpcUnaryClientCodecs}
import smithy4s.protobuf.{ProtobufCodec, ProtobufReadError}
import smithy4s.kinds.PolyFunction5
import weaver.SimpleIOSuite

object GrpcUnaryClientCodecsSpec extends SimpleIOSuite {

  private val pingSchema =
    Schema.operation(ShapeId("test", "Ping"))

  private val pingEndpoint: smithy4s.Endpoint[DummyOp, Unit, Nothing, Unit, Nothing, Nothing] =
    new smithy4s.Endpoint[DummyOp, Unit, Nothing, Unit, Nothing, Nothing] {
      val schema = pingSchema
      def wrap(input: Unit): DummyOp[Unit, Nothing, Unit, Nothing, Nothing] = DummyOp.Ping
    }

  private object DummyService extends smithy4s.Service.Reflective[DummyOp] {
    val id: ShapeId                                               = ShapeId("test", "DummyService")
    val version: String                                           = ""
    val hints: Hints                                              = Hints(Grpc())
    val endpoints: IndexedSeq[smithy4s.Endpoint[DummyOp, _, _, _, _, _]]  = Vector(pingEndpoint)
    def input[I, E, O, SI, SO](op: DummyOp[I, E, O, SI, SO]): I  = op.input
    def ordinal[I, E, O, SI, SO](op: DummyOp[I, E, O, SI, SO]): Int = op.ordinal
  }

  private sealed trait DummyOp[I, E, O, SI, SO] {
    def input: I
    def ordinal: Int
    def endpoint: smithy4s.Endpoint[DummyOp, I, E, O, SI, SO]
  }

  private object DummyOp {
    case object Ping extends DummyOp[Unit, Nothing, Unit, Nothing, Nothing] {
      val input: Unit = ()
      val ordinal: Int = 0
      val endpoint = pingEndpoint
    }
  }

  private val maxMessageSize = 1024

  private val errorPayloadCodec =
    ProtobufCodec.fromSchema(ErrorPayload.schema, ProtobufCodec.createCache())

  private def clientCodecs(strictTrailers: Boolean, strictStatusDetails: Boolean) =
    GrpcUnaryClientCodecs.builder[PolyFunction5.From[DummyOp]#Algebra, IO](DummyService)
      .withMaxMessageSize(maxMessageSize)
      .withStrictTrailers(strictTrailers)
      .withStrictStatusDetails(strictStatusDetails)
      .build()

  test("strictTrailers enforced in outputDecoder") {
    val unary = clientCodecs(strictTrailers = true, strictStatusDetails = false)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    val response = GrpcResponse(Blob.empty, GrpcMetadata.empty, GrpcMetadata.empty, GrpcCompression.Identity)
    unary.outputDecoder(response).attempt.map { result =>
      expect(result == Left(MissingTrailers))
    }
  }

  test("outputDecoder rejects unsupported compression") {
    val unary = clientCodecs(strictTrailers = true, strictStatusDetails = false)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    val trailers = GrpcMetadata.empty.addText(GrpcConstants.grpcStatusHeader, "0")
    val framed   = GrpcFrame.encode(GrpcFrame(compressed = false, Blob.empty))
    val response = GrpcResponse(Blob(framed), GrpcMetadata.empty, trailers, GrpcCompression.Gzip)
    unary.outputDecoder(response).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.UnsupportedCompression("gzip")))
    }
  }

  test("outputDecoder fails on invalid protobuf") {
    val unary = clientCodecs(strictTrailers = true, strictStatusDetails = false)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    val trailers = GrpcMetadata.empty.addText(GrpcConstants.grpcStatusHeader, "0")
    val framed   = GrpcFrame.encode(GrpcFrame(compressed = false, Blob(Array[Byte](1, 2, 3))))
    val response = GrpcResponse(Blob(framed), GrpcMetadata.empty, trailers, GrpcCompression.Identity)
    unary.outputDecoder(response).attempt.map { result =>
      result match {
        case Left(_: ProtobufReadError) => expect(true)
        case _                          => failure("expected ProtobufReadError")
      }
    }
  }

  test("errorDecoder fails on missing grpc-status") {
    val unary    = clientCodecs(strictTrailers = false, strictStatusDetails = false)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    val response = GrpcResponse(Blob.empty, GrpcMetadata.empty, GrpcMetadata.empty, GrpcCompression.Identity)
    unary.errorDecoder(response).attempt.map { result =>
      expect.same(result, Left(MissingTrailers))
    }
  }

  test("errorDecoder fails on invalid grpc-status") {
    val unary    = clientCodecs(strictTrailers = false, strictStatusDetails = false)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    val trailers = GrpcMetadata.empty.addText(GrpcConstants.grpcStatusHeader, "abc")
    val response = GrpcResponse(Blob.empty, GrpcMetadata.empty, trailers, GrpcCompression.Identity)
    unary.errorDecoder(response).attempt.map { result =>
      expect(result == Left(InvalidStatus("abc")))
    }
  }

  test("errorDecoder falls back gracefully when details blob is undecodable") {
    val unary    = clientCodecs(strictTrailers = false, strictStatusDetails = false)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    val trailers = GrpcMetadata.empty
      .addText(GrpcConstants.grpcStatusHeader, "7")
      .addText(GrpcConstants.grpcMessageHeader, GrpcMessageEncoding.encode("denied"))
      .addBinary(GrpcConstants.grpcStatusDetailsHeader, Blob(Array[Byte](1, 2, 3)))
    val response = GrpcResponse(Blob.empty, GrpcMetadata.empty, trailers, GrpcCompression.Identity)
    unary.errorDecoder(response).map { decoded =>
      decoded match {
        case e: RuntimeException =>
          expect(e.getMessage.contains("status=7")) &&
          expect(e.getMessage.contains("denied"))
        case other =>
          failure(s"expected RuntimeException, got ${other.getClass.getName}")
      }
    }
  }

  test("status details mismatch normalizes in permissive mode") {
    val unary = clientCodecs(strictTrailers = false, strictStatusDetails = false)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    val payload = ErrorPayload(code = Some(GrpcStatusCode.INVALID_ARGUMENT.intValue), message = Some("payload"), details = None)
    val trailers = GrpcMetadata.empty
      .addText(GrpcConstants.grpcStatusHeader, "7")
      .addText(GrpcConstants.grpcMessageHeader, GrpcMessageEncoding.encode("denied"))
      .addBinary(GrpcConstants.grpcStatusDetailsHeader, errorPayloadCodec.writeBlob(payload))
    val response = GrpcResponse(Blob.empty, GrpcMetadata.empty, trailers, GrpcCompression.Identity)
    unary.errorDecoder(response).map { decoded =>
      decoded match {
        case e: RuntimeException =>
          expect(e.getMessage.contains("status=7")) &&
          expect(e.getMessage.contains("denied"))
        case other =>
          failure(s"expected RuntimeException, got ${other.getClass.getName}")
      }
    }
  }

  test("status details mismatch fails in strict mode") {
    val unary = clientCodecs(strictTrailers = false, strictStatusDetails = true)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    val payload = ErrorPayload(code = Some(GrpcStatusCode.INVALID_ARGUMENT.intValue), message = Some("payload"), details = None)
    val trailers = GrpcMetadata.empty
      .addText(GrpcConstants.grpcStatusHeader, GrpcStatusCode.PERMISSION_DENIED.intValue.toString)
      .addBinary(GrpcConstants.grpcStatusDetailsHeader, errorPayloadCodec.writeBlob(payload))
    val response = GrpcResponse(Blob.empty, GrpcMetadata.empty, trailers, GrpcCompression.Identity)
    unary.errorDecoder(response).attempt.map { result =>
      expect.same(
        result,
        Left(StatusCodeMismatch(GrpcStatusCode.PERMISSION_DENIED.intValue, GrpcStatusCode.INVALID_ARGUMENT.intValue))
      )
    }
  }

  test("strictStatusDetails accepts matching status codes") {
    val unary = clientCodecs(strictTrailers = false, strictStatusDetails = true)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    val payload = ErrorPayload(code = Some(7), message = Some("payload"), details = None)
    val trailers = GrpcMetadata.empty
      .addText(GrpcConstants.grpcStatusHeader, "7")
      .addBinary(GrpcConstants.grpcStatusDetailsHeader, errorPayloadCodec.writeBlob(payload))
    val response = GrpcResponse(Blob.empty, GrpcMetadata.empty, trailers, GrpcCompression.Identity)
    unary.errorDecoder(response).map { decoded =>
      decoded match {
        case e: RuntimeException =>
          expect(e.getMessage.contains("status=7")) &&
          expect(e.getMessage.contains("payload"))
        case other =>
          failure(s"expected RuntimeException, got ${other.getClass.getName}")
      }
    }
  }

  test("inputEncoder constructs GrpcRequest with correct method path") {
    val unary = clientCodecs(strictTrailers = false, strictStatusDetails = false)
      .apply[Unit, Nothing, Unit, Nothing, Nothing](pingSchema)
    unary.inputEncoder(()).map { request =>
      expect.same(request.path, GrpcMethodPath("test.DummyService", "Ping"))
    }
  }
}
