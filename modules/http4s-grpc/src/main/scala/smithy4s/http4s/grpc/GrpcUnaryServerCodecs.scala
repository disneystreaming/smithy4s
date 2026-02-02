package main.scala.smithy4s.http4s.grpc

import smithy4s.capability.MonadThrowLike
import smithy4s.schema.OperationSchema
import smithy4s.codecs.BlobDecoder
import smithy4s.codecs.BlobEncoder
import smithy4s.http.HttpRequest
import smithy4s.http.HttpResponse
import smithy4s.Blob
import smithy4s.server.UnaryServerCodecs
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.codecs.Decoder
import smithy4s.codecs.PayloadError
import smithy4s.http.HttpContractError
import smithy4s.codecs.Writer
import smithy4s.schema.Schema

object GrpcUnaryServerCodecs {
  def builder[F[_]](implicit F: MonadThrowLike[F]): Builder[F, HttpRequest[Blob], HttpResponse[Blob]] =
    new GrpcUnaryServerCodecsBuilderImpl[F, HttpRequest[Blob], HttpResponse[Blob]](
      baseResponse = _ => F.raiseError(new Exception("Undefined base response")),
      requestBodyDecoders = BlobDecoder.noop,
      successResponseBodyEncoders = BlobEncoder.noop,
      errorResponseBodyEncoders = BlobEncoder.noop,
      requestTransformation = F.pure(_),
      responseTransformation = F.pure(_)
    )

  trait Builder[F[_], Request, Response] {
    def withBaseResponse(f: OperationSchema[_, _, _, _, _] => F[HttpResponse[Blob]]): Builder[F, Request, Response]
    def withBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withSuccessBodyEncoders(decoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withErrorBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withRequestTransformation[Request0](f: Request0 => F[Request]): Builder[F, Request0, Response]
    def withResponseTransformation[Response1](f: Response => F[Response1]): Builder[F, Request, Response1]
    def build(): UnaryServerCodecs.Make[F, Request, Response]
  }

  private case class GrpcUnaryServerCodecsBuilderImpl[F[_], Request, Response](
    baseResponse: OperationSchema[_, _, _, _, _] => F[HttpResponse[Blob]],
    requestBodyDecoders: BlobDecoder.Compiler,
    successResponseBodyEncoders: BlobEncoder.Compiler,
    errorResponseBodyEncoders: BlobEncoder.Compiler,
    requestTransformation: Request => F[HttpRequest[Blob]],
    responseTransformation: HttpResponse[Blob] => F[Response]
  )(implicit F: MonadThrowLike[F])
      extends Builder[F, Request, Response] {
    def withBaseResponse(f: OperationSchema[_, _, _, _, _] => F[HttpResponse[Blob]]): Builder[F, Request, Response] =
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
      val setBody: Writer[HttpResponse[Blob], Blob] = Writer.lift((res, blob) => res.copy(body = blob))
      val setBodyK = smithy4s.codecs.Encoder.pipeToWriterK[HttpResponse[Blob], Blob](setBody)

      val mediaTypeWriters = new CachedSchemaCompiler.Uncached[Writer[HttpResponse[Blob], *]] {
        def fromSchema[A](schema: Schema[A]): Writer[HttpResponse[Blob], A] = {
          new Writer[HttpResponse[Blob], A] {
            def write(request: HttpResponse[Blob], value: A): HttpResponse[Blob] =
              if (request.body.isEmpty) request
              else request.withContentType("application/grpc")
          }
        }
      }

      def responseEncoders(blobEncoders: BlobEncoder.Compiler) = {
        val httpBodyWriters: CachedSchemaCompiler[Writer[HttpResponse[Blob], *]] = 
          blobEncoders.mapK(setBodyK)

        val httpMediaWriters: CachedSchemaCompiler[Writer[HttpResponse[Blob], *]] =
          Writer.combineCompilers(httpBodyWriters, mediaTypeWriters)

        httpMediaWriters
      }

      val inputDecoders: CachedSchemaCompiler[Decoder[F, HttpRequest[Blob], *]] = {
        requestBodyDecoders.mapK(
            Decoder
              .of[Blob]
              .liftPolyFunction(
                MonadThrowLike
                  .liftEitherK[F, PayloadError]
                  .andThen(HttpContractError.fromPayloadErrorK[F])
              )
          ).mapK(Decoder.in[F].composeK[Blob, HttpRequest[Blob]](_.body))
      }

      val outputEncoders = responseEncoders(successResponseBodyEncoders)
      val errorEncoders = responseEncoders(errorResponseBodyEncoders)
      val httpContractErrorWriters = errorEncoders.fromSchema(HttpContractError.schema)

      new UnaryServerCodecs.Make[F, Request, Response] {

        private val inputDecoderCache: inputDecoders.Cache = inputDecoders.createCache()
        private val outputEncoderCache: outputEncoders.Cache = outputEncoders.createCache()
        private val errorEncoderCache: errorEncoders.Cache = errorEncoders.createCache()

        def apply[I, E, O, SI, SO](
            endpoint: OperationSchema[I, E, O, SI, SO]
        ): UnaryServerCodecs[F, Request, Response, I, E, O] = {
          val outputW = outputEncoders.fromSchema(endpoint.output, outputEncoderCache)
          val errorW: Writer[smithy4s.http.HttpResponse[smithy4s.Blob],E] = {
            endpoint.error match {
              case Some(errorSchema) => 
                val errorCodeWriter = Writer.lift[HttpResponse[Blob], E]((response, e) => {
                  response
                    .withStatusCode(500)
                })
                val errorPayloadWriter = Writer.lift[HttpResponse[Blob], E]((response, e) => errorEncoders.fromSchema(errorSchema.schema, errorEncoderCache).write(response, e))
                errorCodeWriter.combine(errorPayloadWriter)
              case None => Writer.noop
            }
          }
          val base = baseResponse(endpoint)
          def encodeOutput(o: O) = F.map(base)(outputW.write(_, o))
          def encodeError(e: E) = F.map(base)(errorW.write(_, e))
          def httpContractErrorEncoder(e: HttpContractError) =
                      F.map(base)(httpContractErrorWriters.write(_, e).withStatusCode(400))

          val inputDecoder: Decoder[F, HttpRequest[Blob], I] =
              inputDecoders.fromSchema(endpoint.input, inputDecoderCache)

          def throwableEncoders(throwable: Throwable): F[HttpResponse[Blob]] =
            throwable match {
              case e: HttpContractError => httpContractErrorEncoder(e)
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
