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

import cats.effect.Concurrent
import cats.implicits._
import smithy4s.{Blob, Service}
import smithy4s.capability.MonadThrowLike
import smithy4s.client.UnaryClientCodecs
import smithy4s.grpc._
import smithy4s.interopcats._
import smithy4s.protobuf.{ProtobufCodec, ProtobufReadError}
import smithy4s.schema.{Alt, CachedSchemaCompiler, ErrorSchema, OperationSchema}

private[grpc] object GrpcUnaryClientCodecs {

  def builder[Alg[_[_, _, _, _, _]], F[_]: Concurrent](
      service: Service[Alg]
  ): Builder[Alg, F] =
    BuilderImpl[Alg, F](
      service            = service,
      compiler           = ProtobufCodec,
      maxMessageSize     = 4 * 1024 * 1024,
      strictTrailers     = false,
      strictStatusDetails = false
    )

  trait Builder[Alg[_[_, _, _, _, _]], F[_]] {
    def withCompiler(c: CachedSchemaCompiler[ProtobufCodec]): Builder[Alg, F]
    def withMaxMessageSize(n: Int): Builder[Alg, F]
    def withStrictTrailers(v: Boolean): Builder[Alg, F]
    def withStrictStatusDetails(v: Boolean): Builder[Alg, F]
    def build(): UnaryClientCodecs.Make[F, GrpcRequest[Blob], GrpcResponse[Blob]]
  }

  private case class BuilderImpl[Alg[_[_, _, _, _, _]], F[_]](
      service: Service[Alg],
      compiler: CachedSchemaCompiler[ProtobufCodec],
      maxMessageSize: Int,
      strictTrailers: Boolean,
      strictStatusDetails: Boolean
  )(implicit F: Concurrent[F])
      extends Builder[Alg, F] {

    def withCompiler(c: CachedSchemaCompiler[ProtobufCodec]): Builder[Alg, F] = copy(compiler = c)
    def withMaxMessageSize(n: Int): Builder[Alg, F]                           = copy(maxMessageSize = n)
    def withStrictTrailers(v: Boolean): Builder[Alg, F]                       = copy(strictTrailers = v)
    def withStrictStatusDetails(v: Boolean): Builder[Alg, F]                  = copy(strictStatusDetails = v)

    def build(): UnaryClientCodecs.Make[F, GrpcRequest[Blob], GrpcResponse[Blob]] =
      new UnaryClientCodecs.Make[F, GrpcRequest[Blob], GrpcResponse[Blob]] {
        def apply[I, E, O, SI, SO](
            schema: OperationSchema[I, E, O, SI, SO]
        ): UnaryClientCodecs[F, GrpcRequest[Blob], GrpcResponse[Blob], I, E, O] = {

          val cache       = compiler.createCache()
          val inputCodec  = compiler.fromSchema(schema.input, cache)
          val outputCodec = compiler.fromSchema(schema.output, cache)

          def inputEncoder(input: I): F[GrpcRequest[Blob]] = {
            val payload = inputCodec.writeBlob(input)
            if (payload.size > maxMessageSize)
              F.raiseError(GrpcFailure.InvalidFrame(GrpcFrame.Error.MessageTooLarge))
            else {
              val frame = GrpcCodecHelpers.encodeUnaryFrame(payload)
              F.pure(
                GrpcRequest(
                  message             = frame,
                  headers             = GrpcMetadata.empty,
                  timeout             = None,
                  compression         = GrpcCompression.Identity,
                  acceptedCompressions = List(GrpcCompression.Identity),
                  path = GrpcMethodPath(
                    s"${service.id.namespace}.${service.id.name}",
                    schema.id.name
                  )
                )
              )
            }
          }

          def outputDecoder(response: GrpcResponse[Blob]): F[O] =
            for {
              _       <- GrpcCodecHelpers.ensureTrailers(response, strictTrailers)
              frame   <- GrpcFrame.decodeUnary[F](response.message, maxMessageSize)
              _ <- F.raiseUnless(response.compression == GrpcCompression.Identity)(
                GrpcFailure.UnsupportedCompression(response.compression.name)
              )
              _ <- F.raiseWhen(frame.compressed)(
                GrpcFailure.UnsupportedCompression(response.compression.name)
              )
              decoded <- MonadThrowLike[F].liftEither(outputCodec.readBlob(frame.message))
            } yield decoded

          val errorPayloadCodec = compiler.fromSchema(ErrorPayload.schema, cache)

          def parseStatus(response: GrpcResponse[Blob]): F[Int] =
            response.trailers.getText(GrpcConstants.grpcStatusHeader).headOption match {
              case None      => F.raiseError(MissingTrailers)
              case Some(raw) =>
                scala.util.Try(raw.toInt).toOption match {
                  case Some(value) => F.pure(value)
                  case None        => F.raiseError(InvalidStatus(raw))
                }
            }

          def decodeMessage(response: GrpcResponse[Blob]): Option[String] =
            response.trailers
              .getText(GrpcConstants.grpcMessageHeader)
              .headOption
              .map(raw => GrpcMessageEncoding.decode(raw).toOption.getOrElse(raw))
              .filter(_.nonEmpty)

          def fallbackError(status: Int, message: Option[String]): Throwable = {
            val base = s"gRPC error status=$status"
            message.filter(_.nonEmpty) match {
              case Some(value) => new RuntimeException(s"$base message=$value")
              case None        => new RuntimeException(base)
            }
          }

          def decodeAlt[A](
              errorSchema: ErrorSchema[E],
              alt: Alt[E, A]
          ): (String, Blob => Either[ProtobufReadError, Throwable]) = {
            val codec   = compiler.fromSchema(alt.schema, cache)
            val typeUrl = GrpcTypeUrl.fromShapeId(alt.schema.shapeId)
            val decode: Blob => Either[ProtobufReadError, Throwable] = blob =>
              codec.readBlob(blob).map(value => errorSchema.unliftError(alt.inject(value)))
            typeUrl -> decode
          }

          val errorDecoders: Map[String, Blob => Either[ProtobufReadError, Throwable]] =
            schema.error
              .map(es => es.alternatives.map(decodeAlt(es, _)).toMap)
              .getOrElse(Map.empty)

          def decodeSimple(response: GrpcResponse[Blob]): F[Throwable] =
            parseStatus(response).map(status => fallbackError(status, decodeMessage(response)))

          def decodeFromPayload(response: GrpcResponse[Blob], errorPayload: ErrorPayload): F[Throwable] = {
            val trailerMessage = decodeMessage(response)
            for {
              status <- parseStatus(response)
              _ <- errorPayload.code match {
                case Some(detailsStatus) if detailsStatus != status && strictStatusDetails =>
                  F.raiseError(StatusCodeMismatch(status, detailsStatus))
                case _ => F.unit
              }
              result <- {
                val message  = trailerMessage.orElse(errorPayload.message)
                val fallback = fallbackError(status, message)
                errorPayload.details.flatMap(_.headOption) match {
                  case Some(detail) =>
                    errorDecoders.get(detail.typeUrl) match {
                      case Some(decode) =>
                        decode(detail.value) match {
                          case Right(decoded) => F.pure(decoded)
                          case Left(_)        => F.pure(fallback)
                        }
                      case None =>
                        if (detail.typeUrl == GrpcTypeUrl.fromShapeId(ProtobufReadError.id)) {
                          val protoCodec = compiler.fromSchema(ProtobufReadError.schema, cache)
                          protoCodec.readBlob(detail.value) match {
                            case Right(decoded) => F.pure(decoded)
                            case Left(_)        => F.pure(fallback)
                          }
                        } else F.pure(fallback)
                    }
                  case None => F.pure(fallback)
                }
              }
            } yield result
          }

          def errorDecoder(response: GrpcResponse[Blob]): F[Throwable] =
            for {
              _         <- GrpcCodecHelpers.ensureTrailers(response, strictTrailers)
              throwable <-
                response.trailers
                  .getBinary(GrpcConstants.grpcStatusDetailsHeader)
                  .headOption
                  .fold(decodeSimple(response)) { blob =>
                    errorPayloadCodec.readBlob(blob) match {
                      case Right(payload) => decodeFromPayload(response, payload)
                      case Left(_)        => decodeSimple(response)
                    }
                  }
            } yield throwable

          new UnaryClientCodecs(inputEncoder, errorDecoder, outputDecoder)
        }
      }
  }
}
