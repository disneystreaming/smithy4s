package smithy4s.grpc

import cats.implicits._
import smithy4s.kinds.PolyFunction
import smithy4s.codecs.{ Decoder => GenericDecoder, Writer }
import smithy4s.grpc.GrpcStatus
import smithy4s.schema.ErrorSchema
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Alt
import smithy4s.schema.Schema
import smithy4s.capability.MonadThrowLike
import smithy4s.grpc.http4s.GrpcHeaders
import smithy4s.Blob
import java.util.Base64
import smithy4s.http.CaseInsensitive

// FIXME: Potentially model this as an ADT with separate success/failure cases
case class GrpcResponse[A](
    status: GrpcStatus,
    metadata: Map[CaseInsensitive, Seq[String]],
    body: A,
  ) {
  def withStatus(status: GrpcStatus) = copy(status = status)
  def withErrorPayload(details: String) = copy(metadata = metadata.updated(CaseInsensitive(GrpcHeaders.StatusDetailsBin.name.toString), Seq(details)))
}

object GrpcResponse {

  private[grpc] type Writer[Body, A] = smithy4s.codecs.Writer[GrpcResponse[Body], A]

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
    private[grpc] def forError[Body, E](maybeErrorSchema: Option[ErrorSchema[E]], encoderCompiler: CachedSchemaCompiler[Writer[Body, *]]): Writer[Body, E] = maybeErrorSchema match {
      case Some(errorSchema) => 
        val dispatcher =
          Alt.Dispatcher(errorSchema.alternatives, errorSchema.ordinal)
        val precompiler = new Alt.Precompiler[Writer[Body, *]] {
          def apply[Err](
              label: String,
              errorSchema: Schema[Err]
          ): Writer[Body, Err] = new Writer[Body, Err] {
            val errorEncoder = encoderCompiler.fromSchema(
              errorSchema,
              encoderCompiler.createCache()
            )

            override def write(message: GrpcResponse[Body], error: Err): GrpcResponse[Body] = {
              val errorStatus = GrpcStatus.Decoder.fromSchema(errorSchema).status(error, GrpcStatus.Internal)

              errorEncoder.write(message, error)
                .withStatus(errorStatus)
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
                // FIXME: We need a generic error to denote that we got an unknown error (i.e. can't decode it).
                // either use smithy4s.http.UnknownErrorResponse or make one specifically for gRPC
                F.raiseError(
                  new RuntimeException("Unknown error.")
                )
            }
          }
        }
      }
    }

  }
}
