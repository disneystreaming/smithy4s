package smithy4s
package grpc
package http4s
package internals

import smithy4s.Blob
import smithy4s.capability.MonadThrowLike
import smithy4s.codecs._
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.OperationSchema
import smithy4s.server.UnaryServerCodecs

object GrpcUnaryServerCodecs {
  def builder[F[_]](implicit F: MonadThrowLike[F]): Builder[F, GrpcRequest[Blob], GrpcResponse[Blob]] =
    new GrpcUnaryServerCodecsBuilderImpl[F, GrpcRequest[Blob], GrpcResponse[Blob]](
      baseResponse = _ => F.raiseError(new Exception("Undefined base response")),
      requestBodyDecoders = BlobDecoder.noop,
      successResponseBodyEncoders = BlobEncoder.noop,
      errorResponseBodyEncoders = BlobEncoder.noop,
      requestTransformation = F.pure(_),
      responseTransformation = F.pure(_)
    )

  trait Builder[F[_], Request, Response] {
    def withBaseResponse(f: OperationSchema[_, _, _, _, _] => F[GrpcResponse[Blob]]): Builder[F, Request, Response]
    def withBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withSuccessBodyEncoders(decoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withErrorBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withRequestTransformation[Request0](f: Request0 => F[Request]): Builder[F, Request0, Response]
    def withResponseTransformation[Response1](f: Response => F[Response1]): Builder[F, Request, Response1]
    def build(): UnaryServerCodecs.Make[F, Request, Response]
  }

  private case class GrpcUnaryServerCodecsBuilderImpl[F[_], Request, Response](
    baseResponse: OperationSchema[_, _, _, _, _] => F[GrpcResponse[Blob]],
    requestBodyDecoders: BlobDecoder.Compiler,
    successResponseBodyEncoders: BlobEncoder.Compiler,
    errorResponseBodyEncoders: BlobEncoder.Compiler,
    requestTransformation: Request => F[GrpcRequest[Blob]],
    responseTransformation: GrpcResponse[Blob] => F[Response]
  )(implicit F: MonadThrowLike[F])
      extends Builder[F, Request, Response] {
    def withBaseResponse(f: OperationSchema[_, _, _, _, _] => F[GrpcResponse[Blob]]): Builder[F, Request, Response] =
      copy(baseResponse = f)
    def withBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response] =
      copy(requestBodyDecoders = decoders)
    def withSuccessBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response] =
      copy(successResponseBodyEncoders = encoders)
    def withErrorBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response] =
      copy(errorResponseBodyEncoders = encoders)
    def withRequestTransformation[Request0](f: Request0 => F[Request]): Builder[F, Request0, Response] =
      copy(requestTransformation = f.andThen(F.flatMap(_)(requestTransformation)))
    def withResponseTransformation[Response1](f: Response => F[Response1]): Builder[F, Request, Response1] =
      copy(responseTransformation = responseTransformation.andThen(F.flatMap(_)(f)))

    def build(): UnaryServerCodecs.Make[F, Request, Response] = {
      val setBodyK = smithy4s.codecs.Encoder.pipeToWriterK[GrpcResponse[Blob], Blob](
        Writer.lift((res, blob) => {
          println(s"Writing response body: ${blob.size}")
          res.copy(body = blob)
        })
      )
      
      val setGrpcStatusDetailsBinK = smithy4s.codecs.Encoder.pipeToWriterK[GrpcResponse[Blob], Blob](
        Writer.lift((res, errorDetailsBlob) => res.withGrpcStatusBin(GrpcHeaders.grpcStatusDetailsBin.value(errorDetailsBlob)))
      )

      val inputDecoders: CachedSchemaCompiler[Decoder[F, GrpcRequest[Blob], *]] =
        requestBodyDecoders
          .mapK(lengthPrefixDecoder)
          .mapK(
            Decoder
              .of[Blob]
              .liftPolyFunction(
                MonadThrowLike
                  .liftEitherK[F, PayloadError]
                  .andThen(GrpcContractError.fromPayloadErrorK[F])
              )
          )
          .mapK(Decoder.in[F].composeK[Blob, GrpcRequest[Blob]](_.body))

      val outputEncoders = 
        successResponseBodyEncoders
          .mapK(lengthPrefixEncoder)
          .mapK(setBodyK)

      val errorEncoders = errorResponseBodyEncoders.mapK(setGrpcStatusDetailsBinK)

      val grpcContractErrorWriters = errorEncoders.fromSchema(GrpcContractError.schema)

      new UnaryServerCodecs.Make[F, Request, Response] {

        private val inputDecoderCache: inputDecoders.Cache = inputDecoders.createCache()
        private val outputEncoderCache: outputEncoders.Cache = outputEncoders.createCache()

        def apply[I, E, O, SI, SO](
            endpoint: OperationSchema[I, E, O, SI, SO]
        ): UnaryServerCodecs[F, Request, Response, I, E, O] = {
          val outputW = outputEncoders.fromSchema(endpoint.output, outputEncoderCache)
          
          val errorW: Writer[GrpcResponse[smithy4s.Blob],E] = 
            GrpcResponse.Encoder.forError(endpoint.error, errorEncoders)

          val base = baseResponse(endpoint)
          def encodeOutput(o: O) = 
              F.map(base)(outputW.write(_, o))
            
          def encodeError(e: E) = F.map(base)(errorW.write(_, e))
          def grpcContractErrorEncoder(e: GrpcContractError) =
            F.map(base)(grpcContractErrorWriters.write(_, e).withStatus(GrpcStatus.InvalidArgument))

          val inputDecoder: Decoder[F, GrpcRequest[Blob], I] =
            inputDecoders.fromSchema(endpoint.input, inputDecoderCache)

          def throwableEncoders(throwable: Throwable): F[GrpcResponse[Blob]] =
            throwable match {
              case e: GrpcContractError => grpcContractErrorEncoder(e)
              case e                    => F.raiseError(e)
            }

          new UnaryServerCodecs(inputDecoder.decode, encodeError, throwableEncoders, encodeOutput)
            .transformRequest(requestTransformation)
            .transformResponse(responseTransformation)
        }
      }
    }
  }

}
