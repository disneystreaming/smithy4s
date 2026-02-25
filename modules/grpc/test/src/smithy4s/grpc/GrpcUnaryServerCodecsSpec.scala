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

import alloy.proto.{GrpcError, GrpcErrorMessage, GrpcStatusCode}
import cats.effect.{Concurrent, IO}
import smithy4s.{Blob, Hints, Newtype, Schema, ShapeId}
import smithy4s.grpc.internals.{GrpcConstants, GrpcUnaryServerCodecs}
import smithy4s.protobuf.{ProtobufCodec, ProtobufReadError}
import weaver.SimpleIOSuite

object GrpcUnaryServerCodecsSpec extends SimpleIOSuite {

  private final case class DynamicError(message: Option[String], other: String)
      extends RuntimeException(message.orNull)

  private object NewtypeMessage extends Newtype[String] {
    val id: ShapeId                      = ShapeId("test", "NewtypeMessage")
    val hints: Hints                     = Hints.empty
    val underlyingSchema: Schema[String] = Schema.string.withId(id).addHints(hints)
    implicit val schema: Schema[NewtypeMessage.Type] =
      smithy4s.schema.Schema.bijection(underlyingSchema, asBijection)
  }

  private type NewtypeMessage = NewtypeMessage.Type

  private final case class NewtypeError(message: Option[NewtypeMessage], other: String)
      extends RuntimeException(message.map(NewtypeMessage.value).orNull)

  private val dynamicErrorSchema: Schema[DynamicError] = {
    val message = Schema.string
      .optional[DynamicError]("message", _.message)
      .addHints(GrpcErrorMessage())
    val other = Schema.string.required[DynamicError]("other", _.other)
    Schema
      .struct(message, other)(DynamicError.apply)
      .withId(ShapeId("test", "DynamicError"))
  }

  private val newtypeErrorSchema: Schema[NewtypeError] = {
    val message = NewtypeMessage.schema
      .optional[NewtypeError]("message", _.message)
      .addHints(GrpcErrorMessage())
    val other = Schema.string.required[NewtypeError]("other", _.other)
    Schema
      .struct(message, other)(NewtypeError.apply)
      .withId(ShapeId("test", "NewtypeError"))
  }

  private val newtypeError =
    newtypeErrorSchema.asError(identity) {
      case e: NewtypeError => Some(e)
      case _               => None
    }

  private val dynamicErrorSchemaWithStatic: Schema[DynamicError] =
    dynamicErrorSchema.addHints(GrpcError(code = 7, message = Some("static")))

  private val dynamicErrorSchemaWithoutStatic: Schema[DynamicError] =
    dynamicErrorSchema.addHints(GrpcError(code = 7, message = None))

  private val dynamicError =
    dynamicErrorSchemaWithStatic.asError(identity) {
      case e: DynamicError => Some(e)
      case _               => None
    }

  private val dynamicErrorNoStatic =
    dynamicErrorSchemaWithoutStatic.asError(identity) {
      case e: DynamicError => Some(e)
      case _               => None
    }

  private def serverCodecs =
    GrpcUnaryServerCodecs.builder[IO]
      .withMaxMessageSize(1024)
      .build()

  private def grpcMessage(response: GrpcResponse[Blob]): Option[String] =
    response.trailers
      .getText(GrpcConstants.grpcMessageHeader)
      .headOption
      .flatMap(GrpcMessageEncoding.decode(_).toOption)

  test("modeled error uses dynamic grpc-message over static") {
    val operation = Schema.operation(ShapeId("test", "Dynamic")).withError(dynamicError)
    val unary     = serverCodecs(operation)
    val error     = DynamicError(message = Some("dynamic"), other = "x")
    unary.errorEncoder(error).map { response =>
      expect.same(grpcMessage(response), Some("dynamic"))
    }
  }

  test("modeled error falls back to static grpc-message") {
    val operation = Schema.operation(ShapeId("test", "Dynamic")).withError(dynamicError)
    val unary     = serverCodecs(operation)
    val error     = DynamicError(message = None, other = "x")
    unary.errorEncoder(error).map { response =>
      expect.same(grpcMessage(response), Some("static"))
    }
  }

  test("modeled error omits grpc-message when none provided") {
    val operation = Schema.operation(ShapeId("test", "DynamicNoStatic")).withError(dynamicErrorNoStatic)
    val unary     = serverCodecs(operation)
    val error     = DynamicError(message = None, other = "x")
    unary.errorEncoder(error).map { response =>
      expect.same(grpcMessage(response), None)
    }
  }

  test("modeled error uses grpc-message for newtype string") {
    val operation = Schema.operation(ShapeId("test", "Newtype")).withError(newtypeError)
    val unary     = serverCodecs(operation)
    val error     = NewtypeError(message = Some(NewtypeMessage("dynamic")), other = "x")
    unary.errorEncoder(error).map { response =>
      expect.same(grpcMessage(response), Some("dynamic"))
    }
  }

  test("throwableEncoder omits details-bin for GrpcFailure") {
    val unary = serverCodecs.apply[Unit, Nothing, Unit, Nothing, Nothing](
      Schema.operation(ShapeId("test", "Ping"))
    )
    unary.throwableEncoder(GrpcFailure.Unimplemented("boom")).map { response =>
      expect(response.trailers.getBinary(GrpcConstants.grpcStatusDetailsHeader).isEmpty)
    }
  }

  test("throwableEncoder maps ProtobufReadError to INVALID_ARGUMENT") {
    val unary = serverCodecs.apply[Unit, Nothing, Unit, Nothing, Nothing](
      Schema.operation(ShapeId("test", "Ping"))
    )
    val error = ProtobufReadError.Other(new RuntimeException("bad proto"))
    unary.throwableEncoder(error).map { response =>
      val status = response.trailers.getText(GrpcConstants.grpcStatusHeader).headOption
      expect.same(status, Some(GrpcStatusCode.INVALID_ARGUMENT.intValue.toString))
    }
  }

  test("throwableEncoder maps unknown throwable to INTERNAL") {
    val unary = serverCodecs.apply[Unit, Nothing, Unit, Nothing, Nothing](
      Schema.operation(ShapeId("test", "Ping"))
    )
    unary.throwableEncoder(new RuntimeException("boom")).map { response =>
      val status = response.trailers.getText(GrpcConstants.grpcStatusHeader).headOption
      expect.same(status, Some(GrpcStatusCode.INTERNAL.intValue.toString))
    }
  }

  test("schemaBasedErrorEncoder uses provided status code") {
    val compiler          = ProtobufCodec
    val cache             = compiler.createCache()
    val statusCode        = GrpcStatusCode.INTERNAL
    val errorPayloadCodec = ProtobufCodec.fromSchema(ErrorPayload.schema, ProtobufCodec.createCache())
    val error: ProtobufReadError = ProtobufReadError.Other(new RuntimeException("boom"))
    GrpcUnaryServerCodecs.schemaBasedErrorEncoder(compiler)(cache, statusCode, error)(
      Concurrent[IO], ProtobufReadError.schema
    ).map { response =>
      val status  = response.trailers.getText(GrpcConstants.grpcStatusHeader).headOption
      val payload = response.trailers
        .getBinary(GrpcConstants.grpcStatusDetailsHeader)
        .headOption
        .flatMap(blob => errorPayloadCodec.readBlob(blob).toOption)
      expect.same(status, Some(statusCode.intValue.toString)) &&
      expect.same(payload.flatMap(_.code), Some(statusCode.intValue))
    }
  }

  test("inputDecoder rejects compressed frames") {
    val unary = serverCodecs.apply[Unit, Nothing, Unit, Nothing, Nothing](
      Schema.operation(ShapeId("test", "Ping"))
    )
    val frame = GrpcFrame.encode(GrpcFrame(compressed = true, Blob.empty))
    val request = GrpcRequest(
      message              = Blob(frame),
      headers              = GrpcMetadata.empty,
      timeout              = None,
      compression          = GrpcCompression.Identity,
      acceptedCompressions = Nil,
      path                 = GrpcMethodPath("test.DummyService", "Ping")
    )
    unary.inputDecoder(request).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.UnsupportedCompression(GrpcCompression.Identity.name)))
    }
  }

  test("inputDecoder rejects unsupported compression") {
    val unary = serverCodecs.apply[Unit, Nothing, Unit, Nothing, Nothing](
      Schema.operation(ShapeId("test", "Ping"))
    )
    val frame = GrpcFrame.encode(GrpcFrame(compressed = false, Blob.empty))
    val request = GrpcRequest(
      message              = Blob(frame),
      headers              = GrpcMetadata.empty,
      timeout              = None,
      compression          = GrpcCompression.Gzip,
      acceptedCompressions = Nil,
      path                 = GrpcMethodPath("test.DummyService", "Ping")
    )
    unary.inputDecoder(request).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.UnsupportedCompression("gzip")))
    }
  }

  test("outputEncoder succeeds") {
    val unary = serverCodecs.apply[Unit, Nothing, Unit, Nothing, Nothing](
      Schema.operation(ShapeId("test", "Ping"))
    )
    unary.outputEncoder(()).map { response =>
      val status = response.trailers.getText(GrpcConstants.grpcStatusHeader).headOption
      expect.same(status, Some(GrpcStatusCode.OK.intValue.toString))
    }
  }

  test("outputEncoder rejects oversized payload") {
    val codecs = GrpcUnaryServerCodecs.builder[IO].withMaxMessageSize(-1).build()
    val unary  = codecs.apply[Unit, Nothing, Unit, Nothing, Nothing](Schema.operation(ShapeId("test", "Ping")))
    unary.outputEncoder(()).attempt.map { result =>
      expect.same(result, Left(GrpcFailure.InvalidFrame(GrpcFrame.Error.MessageTooLarge)))
    }
  }
}
