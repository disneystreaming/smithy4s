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

package smithy4s.grpc.internals

import alloy.proto.{GrpcError, GrpcErrorMessage, GrpcStatusCode}
import cats.effect.Concurrent
import cats.implicits._
import smithy.api.Error
import smithy4s.Blob
import smithy4s.capability.MonadThrowLike
import smithy4s.grpc._
import smithy4s.interopcats._
import smithy4s.protobuf.{ProtobufCodec, ProtobufReadError}
import smithy4s.schema.{Alt, CachedSchemaCompiler, ErrorSchema, OperationSchema, Schema, Primitive}
import smithy4s.schema.Schema.StructSchema
import smithy4s.server.UnaryServerCodecs

private[grpc] object GrpcUnaryServerCodecs {

  def builder[F[_]: Concurrent]: Builder[F] =
    BuilderImpl[F](
      compiler = ProtobufCodec,
      maxMessageSize = 4 * 1024 * 1024
    )

  trait Builder[F[_]] {
    def withCompiler(c: CachedSchemaCompiler[ProtobufCodec]): Builder[F]
    def withMaxMessageSize(n: Int): Builder[F]
    def build(): UnaryServerCodecs.Make[F, GrpcRequest[Blob], GrpcResponse[Blob]]
  }

  /** Encodes an error as a [[GrpcResponse]] with the given status code, encoding
    * the error value itself inside [[ErrorPayload]] `grpc-status-details-bin`.
    * Used to surface internal errors (e.g. [[ProtobufReadError]]) back to the
    * client with a machine-readable payload.
    */
  def schemaBasedErrorEncoder[F[_], Err <: Throwable](
      compiler: CachedSchemaCompiler[ProtobufCodec]
  )(
      cache: compiler.Cache,
      code: GrpcStatusCode,
      error: Err
  )(implicit F: Concurrent[F], schema: Schema[Err]): F[GrpcResponse[Blob]] = {
    val errorCodec        = compiler.fromSchema(schema, cache)
    val errorPayloadCodec = compiler.fromSchema(ErrorPayload.schema, cache)
    val errorPayload = ErrorPayload(
      code = Some(code.intValue),
      message = Some(error.getMessage()),
      details = Some(List(StatusDetails(
        typeUrl = GrpcTypeUrl.fromShapeId(ProtobufReadError.id),
        value   = errorCodec.writeBlob(error)
      )))
    )
    val trailers =
      GrpcMetadata.empty
        .addText(GrpcConstants.grpcStatusHeader, code.intValue.toString())
        .addBinary(GrpcConstants.grpcStatusDetailsHeader, errorPayloadCodec.writeBlob(errorPayload))
    F.pure(GrpcResponse(Blob.empty, GrpcMetadata.empty, trailers, GrpcCompression.Identity))
  }

  private case class BuilderImpl[F[_]](
      compiler: CachedSchemaCompiler[ProtobufCodec],
      maxMessageSize: Int
  )(implicit F: Concurrent[F])
      extends Builder[F] {

    def withCompiler(c: CachedSchemaCompiler[ProtobufCodec]): Builder[F] = copy(compiler = c)
    def withMaxMessageSize(n: Int): Builder[F]                           = copy(maxMessageSize = n)

    def build(): UnaryServerCodecs.Make[F, GrpcRequest[Blob], GrpcResponse[Blob]] =
      new UnaryServerCodecs.Make[F, GrpcRequest[Blob], GrpcResponse[Blob]] {
        def apply[I, E, O, SI, SO](
            schema: OperationSchema[I, E, O, SI, SO]
        ): UnaryServerCodecs[F, GrpcRequest[Blob], GrpcResponse[Blob], I, E, O] = {

          val cache       = compiler.createCache()
          val inputCodec  = compiler.fromSchema(schema.input, cache)
          val outputCodec = compiler.fromSchema(schema.output, cache)

          def inputDecoder(request: GrpcRequest[Blob]): F[I] =
            for {
              frame <- GrpcFrame.decodeUnary[F](request.message, maxMessageSize)
              _ <- F.raiseUnless(request.compression == GrpcCompression.Identity)(
                GrpcFailure.UnsupportedCompression(request.compression.name)
              )
              _ <- F.raiseWhen(frame.compressed)(
                GrpcFailure.UnsupportedCompression(request.compression.name)
              )
              decoded <- MonadThrowLike[F].liftEither(inputCodec.readBlob(frame.message))
            } yield decoded

          def errorEncoder(error: E): F[GrpcResponse[Blob]] =
            schema.error match {
              case Some(errorSchema) =>
                F.pure(buildErrorDispatch(errorSchema, compiler)(cache)(error))
              case None =>
                F.pure(GrpcResponse(Blob.empty, GrpcMetadata.empty, GrpcCodecHelpers.internalErrorTrailers, GrpcCompression.Identity))
            }

          def throwableEncoder(throwable: Throwable): F[GrpcResponse[Blob]] =
            throwable match {
              case e: ProtobufReadError =>
                schemaBasedErrorEncoder(compiler)(cache, GrpcStatusCode.INVALID_ARGUMENT, e)
              case e: GrpcFailure =>
                val trailers =
                  GrpcMetadata.empty
                    .addText(GrpcConstants.grpcStatusHeader, e.status.intValue.toString())
                    .addText(GrpcConstants.grpcMessageHeader, e.message)
                F.pure(GrpcResponse(Blob.empty, GrpcMetadata.empty, trailers, GrpcCompression.Identity))
              case _ =>
                val trailers =
                  GrpcMetadata.empty
                    .addText(GrpcConstants.grpcStatusHeader, GrpcStatusCode.INTERNAL.intValue.toString())
                    .addText(GrpcConstants.grpcMessageHeader, GrpcMessageEncoding.encode("Unhandled gRPC error"))
                F.pure(GrpcResponse(Blob.empty, GrpcMetadata.empty, trailers, GrpcCompression.Identity))
            }

          def outputEncoder(output: O): F[GrpcResponse[Blob]] = {
            val payload = outputCodec.writeBlob(output)
            if (payload.size > maxMessageSize)
              F.raiseError(GrpcFailure.InvalidFrame(GrpcFrame.Error.MessageTooLarge))
            else {
              val frame = GrpcCodecHelpers.encodeUnaryFrame(payload)
              F.pure(GrpcResponse(frame, GrpcMetadata.empty, GrpcCodecHelpers.successTrailers, GrpcCompression.Identity))
            }
          }

          new UnaryServerCodecs(inputDecoder, errorEncoder, throwableEncoder, outputEncoder)
        }
      }
  }

  private def buildErrorDispatch[E](
      errorSchema: ErrorSchema[E],
      compiler: CachedSchemaCompiler[ProtobufCodec]
  )(cache: compiler.Cache): E => GrpcResponse[Blob] = {
    val dispatcher        = Alt.Dispatcher(errorSchema.alternatives, errorSchema.ordinal)
    val errorPayloadCodec = compiler.fromSchema(ErrorPayload.schema, cache)
    val precompiler = new Alt.Precompiler[* => GrpcResponse[Blob]] {
      def apply[A](label: String, altSchema: Schema[A]): A => GrpcResponse[Blob] = {
        val codec          = compiler.fromSchema(altSchema, cache)
        val typeUrl        = GrpcTypeUrl.fromShapeId(altSchema.shapeId)
        val status         = grpcStatusFromSchema(altSchema)
        val dynamicMessage = grpcErrorMessageAccessor(altSchema)
        val staticMessage  = altSchema.hints.get(GrpcError).flatMap(_.message)
        (a: A) => {
          val bytes   = codec.writeBlob(a)
          val details = StatusDetails(typeUrl = typeUrl, value = bytes)
          val message = dynamicMessage(a).filter(_.nonEmpty).orElse(staticMessage.filter(_.nonEmpty))
          val payload = ErrorPayload(
            code    = Some(status),
            message = message,
            details = Some(List(details))
          )
          GrpcResponse(
            Blob.empty,
            GrpcMetadata.empty,
            GrpcCodecHelpers.buildTrailers(status, message, Some(payload), errorPayloadCodec.writeBlob),
            GrpcCompression.Identity
          )
        }
      }
    }
    dispatcher.compile(precompiler)
  }

  private def grpcErrorMessageAccessor[A](schema: Schema[A]): A => Option[String] = {
    def extractor[B](schema: Schema[B]): Option[B => Option[String]] = schema match {
      case Schema.PrimitiveSchema(_, _, Primitive.PString) =>
        Some((b: B) => Option(b.asInstanceOf[String]))
      case Schema.OptionSchema(underlying) =>
        extractor(underlying).map { get =>
          (b: B) => b.asInstanceOf[Option[Any]].flatMap(v => get(v.asInstanceOf[Any]))
        }
      case Schema.BijectionSchema(underlying, bijection) =>
        extractor(underlying).map { get => (b: B) => get(bijection.from(b)) }
      case Schema.RefinementSchema(underlying, refinement) =>
        extractor(underlying).map { get => (b: B) => get(refinement.from(b)) }
      case Schema.LazySchema(suspend) =>
        extractor(suspend.value)
      case _ => None
    }

    schema match {
      case StructSchema(_, _, fields, _) =>
        fields
          .collectFirst {
            case field if field.memberHints.has(GrpcErrorMessage) =>
              extractor(field.schema).map(get => (a: A) => get(field.get(a)))
          }
          .flatten
          .getOrElse((_: A) => None)
      case _ => (_: A) => None
    }
  }

  private def grpcStatusFromSchema(schema: Schema[_]): Int =
    schema.hints
      .get(GrpcError)
      .map(_.code)
      .orElse(
        schema.hints.get(Error).map {
          case Error.CLIENT => GrpcStatusCode.INVALID_ARGUMENT.intValue
          case Error.SERVER => GrpcStatusCode.INTERNAL.intValue
        }
      )
      .getOrElse(GrpcStatusCode.UNKNOWN.intValue)
}
