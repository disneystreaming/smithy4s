package smithy4s
package grpc
package http4s
package internals

import smithy4s.capability.MonadThrowLike
import smithy4s.Blob
import smithy4s.client.UnaryClientCodecs
import smithy4s.schema.OperationSchema
import smithy4s.codecs._
import smithy4s.grpc.{ GrpcRequest, GrpcResponse }
import smithy4s.grpc.GrpcContractError

object GrpcUnaryClientCodecs {
  def builder[F[_]](implicit F: MonadThrowLike[F]): Builder[F, GrpcRequest[Blob], GrpcResponse[Blob]] = 
    new GrpcUnaryClientCodecsBuilderImpl[F, GrpcRequest[Blob], GrpcResponse[Blob]](
      baseRequest = _ => F.raiseError(new Exception("Undefined base request")),
      requestBodyEncoders = BlobEncoder.noop,
      successResponseBodyDecoders = BlobDecoder.noop,
      errorResponseBodyDecoders = BlobDecoder.noop,
      errorDiscriminator = (_: GrpcResponse[Blob]) => F.pure(GrpcDiscriminator.Undetermined),
      requestTransformation = F.pure(_),
      responseTransformation = F.pure(_),
    )

  trait Builder[F[_], Request, Response] {
    def withBaseRequest(f: OperationSchema[_, _, _, _, _] => F[GrpcRequest[Blob]]): Builder[F, Request, Response]
    def withBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withSuccessBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withErrorBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withErrorDiscriminator(discriminator: GrpcResponse[Blob] => F[GrpcDiscriminator]): Builder[F, Request, Response]
    def withRequestTransformation[Request1](f: Request => F[Request1]): Builder[F, Request1, Response]
    def withResponseTransformation[Response0](f: Response0 => F[Response]): Builder[F, Request, Response0]
    def build(): UnaryClientCodecs.Make[F, Request, Response]
  }

  private case class GrpcUnaryClientCodecsBuilderImpl[F[_], Request, Response](
    baseRequest: OperationSchema[_, _, _, _, _] => F[GrpcRequest[Blob]],
    requestBodyEncoders: BlobEncoder.Compiler,
    successResponseBodyDecoders: BlobDecoder.Compiler,
    errorResponseBodyDecoders: BlobDecoder.Compiler,
    errorDiscriminator: GrpcResponse[Blob] => F[GrpcDiscriminator],
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

        def withErrorDiscriminator(discriminator: GrpcResponse[Blob] => F[GrpcDiscriminator]): Builder[F,Request,Response] =
          copy(errorDiscriminator = discriminator)

        def withRequestTransformation[Request1](f: Request => F[Request1]): Builder[F, Request1, Response] =
          copy(requestTransformation = requestTransformation.andThen(F.flatMap(_)(f)))

        def withResponseTransformation[Response0](f: Response0 => F[Response]): Builder[F, Request, Response0] =
          copy(responseTransformation = f.andThen(F.flatMap(_)(responseTransformation)))

        override def build(): UnaryClientCodecs.Make[F,Request,Response] = {
          val setBody: Writer[GrpcRequest[Blob], Blob] = Writer.lift((req, blob) => req.copy(body = blob))
          val setBodyK = smithy4s.codecs.Encoder.pipeToWriterK[GrpcRequest[Blob], Blob](setBody)

          val grpcBodyWriter = 
            requestBodyEncoders
              .mapK(lengthPrefixEncoder)
              .mapK(setBodyK)
          
          val grpcBodyDecoder = successResponseBodyDecoders
          .mapK(lengthPrefixDecoder)
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
                errorDiscriminator,
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
