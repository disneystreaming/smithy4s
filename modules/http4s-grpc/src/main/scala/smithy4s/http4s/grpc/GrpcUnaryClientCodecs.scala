package smithy4s.http4s.grpc

import smithy4s.capability.MonadThrowLike
import smithy4s.http.HttpRequest
import smithy4s.Blob
import smithy4s.client.UnaryClientCodecs
import smithy4s.schema.OperationSchema
import smithy4s.http.HttpResponse
import smithy4s.codecs.BlobDecoder
import smithy4s.codecs.BlobEncoder
import smithy4s.codecs.Writer
import smithy4s.codecs.Decoder
import smithy4s.codecs.PayloadError
import smithy4s.http.HttpContractError

object GrpcUnaryClientCodecs {
  def builder[F[_]](implicit F: MonadThrowLike[F]): Builder[F, HttpRequest[Blob], HttpResponse[Blob]] =
    GrpcUnaryClientCodecsBuilderImpl(
      baseRequest = _ => F.raiseError(new Exception("Undefined base request")),
      requestBodyEncoders = BlobEncoder.noop,
      successResponseBodyDecoders = BlobDecoder.noop,
      errorResponseBodyDecoders = BlobDecoder.noop,
      requestTransformation = F.pure(_),
      responseTransformation = F.pure(_),
    )

  trait Builder[F[_], Request, Response] {
    def withBaseRequest(f: OperationSchema[_, _, _, _, _] => F[HttpRequest[Blob]]): Builder[F, Request, Response]
    def withBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withSuccessBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withErrorBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withRequestTransformation[Request1](f: Request => F[Request1]): Builder[F, Request1, Response]
    def withResponseTransformation[Response0](f: Response0 => F[Response]): Builder[F, Request, Response0]
    def build(): UnaryClientCodecs.Make[F, Request, Response]
  }

  private case class GrpcUnaryClientCodecsBuilderImpl[F[_], Request, Response](
    baseRequest: OperationSchema[_, _, _, _, _] => F[HttpRequest[Blob]],
    requestBodyEncoders: BlobEncoder.Compiler,
    successResponseBodyDecoders: BlobDecoder.Compiler,
    errorResponseBodyDecoders: BlobDecoder.Compiler,
    requestTransformation: HttpRequest[Blob] => F[Request],
    responseTransformation: Response => F[HttpResponse[Blob]],
    contentType: String = "application/grpc",
    acceptMediaType: String = "application/grpc",
  )(implicit F: MonadThrowLike[F])
      extends Builder[F, Request, Response] {

    def withBaseRequest(f: OperationSchema[_, _, _, _, _] => F[HttpRequest[Blob]]): Builder[F, Request, Response] =
      copy(baseRequest = f)

    def withBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response] =
      copy(requestBodyEncoders = encoders)

    def withSuccessBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response] =
      copy(successResponseBodyDecoders = decoders)

    def withErrorBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response] =
      copy(errorResponseBodyDecoders = decoders)

    def withRequestTransformation[Request1](f: Request => F[Request1]): Builder[F, Request1, Response] =
      copy(requestTransformation = requestTransformation.andThen(F.flatMap(_)(f)))

    def withResponseTransformation[Response0](f: Response0 => F[Response]): Builder[F, Request, Response0] =
      copy(responseTransformation = f.andThen(F.flatMap(_)(responseTransformation)))

    override def build(): UnaryClientCodecs.Make[F,Request,Response] = {
      val setBody: Writer[HttpRequest[Blob], Blob] = Writer.lift((req, blob) => req.copy(body = blob))
      val setBodyK = smithy4s.codecs.Encoder.pipeToWriterK[HttpRequest[Blob], Blob](setBody)
      val grpcBodyWriter = requestBodyEncoders.mapK(setBodyK)

      val responseBodyDecoder = successResponseBodyDecoders.mapK(
            Decoder
              .of[Blob]
              .liftPolyFunction(
                MonadThrowLike
                  .liftEitherK[F, PayloadError]
                  .andThen(HttpContractError.fromPayloadErrorK[F])
              )
          ).mapK(HttpResponse.extractBody[F, Blob])

      new UnaryClientCodecs.Make[F, Request, Response] {

        private val inputEncoderCache = grpcBodyWriter.createCache()
        private val outputDecoderCache = responseBodyDecoder.createCache()

        def apply[I, E, O, SI, SO](
            endpoint: OperationSchema[I, E, O, SI, SO]
        ): UnaryClientCodecs[F, Request, Response, I, E, O] = {
          val endpointName = endpoint.id.name
          val uriWriter = Writer.lift((req: HttpRequest[Blob], _: I) => req.copy(uri = req.uri.copy(path = req.uri.path :+ endpointName)))
          val inputWriter = grpcBodyWriter.fromSchema(endpoint.input, inputEncoderCache)
          val contentTypeHeaderWriter = Writer.lift((req: HttpRequest[Blob], _: I) => req.withContentType(contentType))
          val acceptHeaderWriter = Writer.lift((req: HttpRequest[Blob], _: I) => req.withAccept(acceptMediaType))

          val finalInputWriter = inputWriter.combine(contentTypeHeaderWriter).combine(acceptHeaderWriter).combine(uriWriter)

          val inputEncoder = (i: I) => { F.map(baseRequest(endpoint))(finalInputWriter.write(_, i)) }

          def errorDecoder = Decoder.lift[F, HttpResponse[Blob], Throwable](_ => ???)
          
          def outputDecoder:Decoder[F, HttpResponse[Blob], O] = responseBodyDecoder.fromSchema(endpoint.output, outputDecoderCache)

          new UnaryClientCodecs(inputEncoder, errorDecoder.decode, outputDecoder.decode)
            .transformRequest[Request](requestTransformation)
            .transformResponse[Response](responseTransformation)
        }
      }
    }

  }
}
