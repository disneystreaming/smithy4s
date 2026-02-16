package smithy4s.grpc

import cats.implicits._
import smithy4s.kinds.PolyFunction
import smithy4s.codecs.{ Decoder => GenericDecoder, Encoder => CodecEncoder, Writer }
import smithy4s.schema.ErrorSchema
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Alt
import smithy4s.schema.Schema
import smithy4s.capability.MonadThrowLike
import smithy4s.grpc.http4s.GrpcHeaders
import smithy4s.Blob
import java.util.Base64
import smithy4s.http.CaseInsensitive
import alloy.proto.GrpcError
import alloy.proto.StatusDetails
import alloy.proto.StatusDetailsEntry

// FIXME: Potentially model this as an ADT with separate success/failure cases
case class GrpcResponse[A](
  status: Status,
  metadata: Map[CaseInsensitive, Seq[String]],
  body: A
) {
  def withStatus(status: Status) = copy(status = status)
}

object GrpcResponse {

  private[grpc] type ResponseWriter[Body, A] = smithy4s.codecs.Writer[GrpcResponse[Body], A]

  private[grpc] type ResponseDecoder[F[_], Body, A] = smithy4s.codecs.Decoder[F, GrpcResponse[Body], A]

  private[grpc] def extractBody[F[_], Body]
          : PolyFunction[GenericDecoder[F, Body, *], ResponseDecoder[F, Body, *]] =
        GenericDecoder.in[F].composeK(_.body)

  private[grpc] def extractErrorAsBlob[F[_], Body](implicit F: MonadThrowLike[F])
      : ResponseDecoder[F, Blob, Blob] =
        new ResponseDecoder[F, Blob, Blob] {
          override def decode(resp: GrpcResponse[Blob]): F[Blob] = {
            val header = GrpcHeaders.StatusDetailsBin.name
            resp.metadata.get(CaseInsensitive(header.toString))
              .filter(_.nonEmpty)
              .map {
                case error :: _ =>
                  Either.catchNonFatal(Base64.getDecoder().decode(error))
                    .map(bytes => F.pure(Blob(bytes)))
                    .valueOr(t => F.raiseError[Blob](new RuntimeException(s"${header.toString} is not a base64 encoded string")))
              }.getOrElse(F.pure(Blob.empty))
          }
        }

  object Encoder {
    private[grpc] def forError[E](
      maybeErrorSchema: Option[ErrorSchema[E]],
      encoderCompiler: CachedSchemaCompiler[CodecEncoder[Blob, *]],
      defaultStatusCode: StatusCode
    ): ResponseWriter[Blob, E] = maybeErrorSchema match {
      case Some(errorSchema) =>
        val dispatcher =
          Alt.Dispatcher(errorSchema.alternatives, errorSchema.ordinal)
        val precompiler = new Alt.Precompiler[ResponseWriter[Blob, *]] {

          def apply[Err](
              label: String,
              errorSchema: Schema[Err]
          ): ResponseWriter[Blob, Err] = new ResponseWriter[Blob, Err] {
            val errorEncoder = encoderCompiler.fromSchema(errorSchema, encoderCompiler.createCache())

            override def write(response: GrpcResponse[Blob], error: Err): GrpcResponse[Blob] = {
              val (code, message) = errorSchema.hints.get(GrpcError) match {
                case Some(hint) =>
                  StatusCode.fromStatusCode(hint.errorCode.intValue) -> hint.message
                case None =>
                  defaultStatusCode -> Option.empty
              }

              val errorPayload = StatusDetailsEntry(
                typeUrl = TypeUrl.fromShapeId(errorSchema.shapeId),
                bytes = errorEncoder.encode(error)
              )
              response.withStatus(Status(code = code, message = message, details = StatusDetails(List(errorPayload))))
            }
          }
        }
        dispatcher.compile(precompiler)
      case None => Writer.noop
    }
  }

  object Decoder {
    /**
    * Creates a response decoder that dispatches the response to
    * the correct alternative, based on some discriminator, and
    * then upcasts the error as a throwable
    */
    private[grpc] def forErrorAsThrowable[F[_], Body, E](
        maybeErrorSchema: Option[ErrorSchema[E]],
        decoderCompiler: CachedSchemaCompiler[ResponseDecoder[F, Body, *]],
        discriminate: GrpcResponse[Body] => F[GrpcDiscriminator],
    )(implicit F: MonadThrowLike[F]): ResponseDecoder[F, Body, Throwable] = {
      new ResponseDecoder[F, Body, Throwable] {
        override def decode(response: GrpcResponse[Body]): F[Throwable] = {
          F.flatMap(discriminate(response)){ discriminator =>
            GrpcErrorSelector.asThrowable(maybeErrorSchema, decoderCompiler).apply(discriminator) match {
              case Some(decoder) =>
                decoder.decode(response)
              case None =>
                F.raiseError(UnknownGrpcError.fromGrpcStatus(response.status))
            }
          }
        }
      }
    }

  }
}
