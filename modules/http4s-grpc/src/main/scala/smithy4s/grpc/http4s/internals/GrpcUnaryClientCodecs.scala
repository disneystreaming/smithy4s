package smithy4s
package grpc
package http4s
package internals

import smithy4s.capability.MonadThrowLike
import smithy4s.Blob
import smithy4s.client.UnaryClientCodecs
import smithy4s.schema.OperationSchema
import smithy4s.codecs.BlobDecoder
import smithy4s.codecs.BlobEncoder
import smithy4s.codecs.Writer
import smithy4s.codecs.Decoder
import smithy4s.codecs.PayloadError
import smithy4s.grpc.{ GrpcRequest, GrpcResponse }
import smithy4s.grpc.GrpcContractError

object GrpcUnaryClientCodecs {
  def builder[F[_]](implicit F: MonadThrowLike[F]): Builder[F, GrpcRequest[Blob], GrpcResponse[Blob]] = 
    new GrpcUnaryClientCodecsBuilderImpl[F, GrpcRequest[Blob], GrpcResponse[Blob]](
      baseRequest = _ => F.raiseError(new Exception("Undefined base request")),
      requestBodyEncoders = BlobEncoder.noop,
      successResponseBodyDecoders = BlobDecoder.noop,
      errorResponseBodyDecoders = BlobDecoder.noop,
      requestTransformation = F.pure(_),
      responseTransformation = F.pure(_),
    )

  trait Builder[F[_], Request, Response] {
    def withBaseRequest(f: OperationSchema[_, _, _, _, _] => F[GrpcRequest[Blob]]): Builder[F, Request, Response]
    def withBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withSuccessBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withErrorBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withRequestTransformation[Request1](f: Request => F[Request1]): Builder[F, Request1, Response]
    def withResponseTransformation[Response0](f: Response0 => F[Response]): Builder[F, Request, Response0]
    def build(): UnaryClientCodecs.Make[F, Request, Response]
  }

  private case class GrpcUnaryClientCodecsBuilderImpl[F[_], Request, Response](
    baseRequest: OperationSchema[_, _, _, _, _] => F[GrpcRequest[Blob]],
    requestBodyEncoders: BlobEncoder.Compiler,
    successResponseBodyDecoders: BlobDecoder.Compiler,
    errorResponseBodyDecoders: BlobDecoder.Compiler,
    requestTransformation: GrpcRequest[Blob] => F[Request],
    responseTransformation: Response => F[GrpcResponse[Blob]],
  ) (implicit F: MonadThrowLike[F])
      extends Builder[F, Request, Response] {

        def withBaseRequest(f: OperationSchema[_, _, _, _, _] => F[GrpcRequest[Blob]]): Builder[F, Request, Response] =
          copy(baseRequest = f)

        def withBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response] =
          copy(requestBodyEncoders = encoders)

        def withSuccessBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response] =
          copy(successResponseBodyDecoders = decoders)

        def withErrorBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F,Request,Response] =
          copy(errorResponseBodyDecoders = decoders)

        def withRequestTransformation[Request1](f: Request => F[Request1]): Builder[F, Request1, Response] =
          copy(requestTransformation = requestTransformation.andThen(F.flatMap(_)(f)))

        def withResponseTransformation[Response0](f: Response0 => F[Response]): Builder[F, Request, Response0] =
          copy(responseTransformation = f.andThen(F.flatMap(_)(responseTransformation)))

        override def build(): UnaryClientCodecs.Make[F,Request,Response] = {
          // val lengthPrefixEncoder = smithy4s.codecs.Encoder.andThenK[Blob, Blob]{ payloadBlob => 
          //   val PAYLOAD_PREFIX_SIZE = 5
          //   val compressionFlag: Byte = 0 //if (compressed) 1 else 0
          //   val messageLength = payloadBlob.size

          //   // Create 5-byte header + message
          //   val messageWithGrpcPrefix = 
          //     ByteBuffer.allocate(PAYLOAD_PREFIX_SIZE + messageLength)
          //       .put(compressionFlag)           // 1 byte: compression flag
          //       .putInt(messageLength)          // 4 bytes: message length (big-endian)
          //       .put(payloadBlob.toArray)       // N bytes: actual protobuf message
          //       .rewind().asInstanceOf[ByteBuffer]

          //   Blob(messageWithGrpcPrefix)
          // }
          val setBody: Writer[GrpcRequest[Blob], Blob] = Writer.lift((req, blob) => req.copy(body = blob))
          val setBodyK = smithy4s.codecs.Encoder.pipeToWriterK[GrpcRequest[Blob], Blob](setBody)

          // val setBodyWithLengthPrefix = lengthPrefixEncoder.andThen(setBodyK)
          val setBodyWithLengthPrefix = setBodyK //FIXME: figure out if we add the length prefix here or in the encoders
          val grpcBodyWriter = requestBodyEncoders.mapK(setBodyWithLengthPrefix)

          // val payloadDecoder = successResponseBodyDecoders.mapK(new PolyFunction[BlobDecoder, BlobDecoder]{
          //   override def apply[A0](fa: BlobDecoder[A0]): BlobDecoder[A0] = new BlobDecoder[A0] {
          //     override def decode(in: Blob): Either[PayloadError,A0] = {
          //       val newBufferSize = in.size - 5
          //       val bb = ByteBuffer.allocate(newBufferSize)
          //       in.copyToBuffer(bb, 5, newBufferSize)
          //       fa.decode(Blob(bb))
          //     }
          //   }
          // })
          val payloadDecoder = successResponseBodyDecoders
          
          val grpcBodyDecoder = payloadDecoder
          .mapK(
            Decoder
              .of[Blob]
              .liftPolyFunction(
                MonadThrowLike
                  .liftEitherK[F, PayloadError]
                  .andThen(GrpcContractError.fromPayloadErrorK[F])
              )
          ).mapK(GrpcResponse.extractBody[F, Blob])

          val errorDecoders =  errorResponseBodyDecoders
          .mapK(
            Decoder
              .of[Blob]
              .liftPolyFunction(
                MonadThrowLike
                  .liftEitherK[F, PayloadError]
                  .andThen(GrpcContractError.fromPayloadErrorK[F])
              )
          ).mapK(GrpcResponse.extractBody[F, Blob])

          new UnaryClientCodecs.Make[F, Request, Response] {
            val grpcBodyWriterCache = grpcBodyWriter.createCache()
            val grpcBodyDecoderCache = grpcBodyDecoder.createCache()

            override def apply[I, E, O, SI, SO](endpoint: OperationSchema[I,E,O,SI,SO]): UnaryClientCodecs[F,Request,Response,I,E,O] = {
              val inputEncoder = {
                val finalInputWriter = grpcBodyWriter.fromSchema(endpoint.input, grpcBodyWriterCache)
                (i: I) => { F.map(baseRequest(endpoint))(finalInputWriter.write(_, i)) }
              }
              def errorDecoder = GrpcResponse.Decoder.forErrorAsThrowable[F, Blob, E](
                endpoint.error,
                errorDecoders,
                (resp: GrpcResponse[Blob]) => F.pure(GrpcDiscriminator.StatusCode(resp.status.code))
              )

              def outputDecoder = grpcBodyDecoder.fromSchema(endpoint.output, grpcBodyDecoderCache)

              new UnaryClientCodecs(inputEncoder, errorDecoder.decode, outputDecoder.decode)
                .transformRequest[Request](requestTransformation)
                .transformResponse[Response](responseTransformation)
            }
          }
        }
      }
}
