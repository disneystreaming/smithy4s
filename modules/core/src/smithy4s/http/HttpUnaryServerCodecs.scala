/*
 *  Copyright 2021-2022 Disney Streaming
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

package smithy4s
package http

import smithy4s.server.UnaryServerCodecs
import smithy4s.codecs.{BlobEncoder, BlobDecoder}
import smithy4s.capability.MonadThrowLike
import smithy4s.codecs.Writer
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.OperationSchema
import smithy4s.codecs.Reader
import smithy4s.codecs.PayloadError

// scalafmt: {maxColumn = 120}
object HttpUnaryServerCodecs {

  def builder[F[_]](implicit F: MonadThrowLike[F]): Builder[F, HttpRequest[Blob], HttpResponse[Blob]] =
    HttpUnaryClientCodecsBuilderImpl[F, HttpRequest[Blob], HttpResponse[Blob]](
      baseResponse = _ => F.raiseError(new Exception("Undefined base response")),
      requestBodyDecoders = BlobDecoder.noop,
      successResponseBodyEncoders = BlobEncoder.noop,
      errorResponseBodyEncoders = BlobEncoder.noop,
      metadataEncoders = None,
      metadataDecoders = None,
      rawStringsAndBlobPayloads = false,
      writeEmptyStructs = _ => false,
      errorTypeHeaders = Nil,
      responseMediaType = "text/plain",
      requestTransformation = F.pure(_),
      responseTransformation = F.pure(_)
    )

  trait Builder[F[_], Request, Response] {
    def withBaseResponse(f: OperationSchema[_, _, _, _, _] => F[HttpResponse[Blob]]): Builder[F, Request, Response]
    def withBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withSuccessBodyEncoders(decoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withErrorBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withMetadataEncoders(encoders: Metadata.Encoder.Compiler): Builder[F, Request, Response]
    def withMetadataDecoders(decoders: Metadata.Decoder.Compiler): Builder[F, Request, Response]
    def withRawStringsAndBlobsPayloads: Builder[F, Request, Response]
    def withWriteEmptyStructs(cond: Schema[_] => Boolean): Builder[F, Request, Response]
    def withResponseMediaType(mediaType: String): Builder[F, Request, Response]
    def withErrorTypeHeaders(headerNames: String*): Builder[F, Request, Response]
    def withRequestTransformation[Request0](f: Request0 => F[Request]): Builder[F, Request0, Response]
    def withResponseTransformation[Response1](f: Response => F[Response1]): Builder[F, Request, Response1]
    def build(): UnaryServerCodecs.Make[F, Request, Response]
  }

  private case class HttpUnaryClientCodecsBuilderImpl[F[_], Request, Response](
      baseResponse: OperationSchema[_, _, _, _, _] => F[HttpResponse[Blob]],
      requestBodyDecoders: BlobDecoder.Compiler,
      successResponseBodyEncoders: BlobEncoder.Compiler,
      errorResponseBodyEncoders: BlobEncoder.Compiler,
      metadataEncoders: Option[Metadata.Encoder.Compiler],
      metadataDecoders: Option[Metadata.Decoder.Compiler],
      rawStringsAndBlobPayloads: Boolean,
      writeEmptyStructs: Schema[_] => Boolean,
      responseMediaType: String,
      errorTypeHeaders: List[String],
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
    def withMetadataEncoders(encoders: Metadata.Encoder.Compiler): Builder[F, Request, Response] =
      copy(metadataEncoders = Some(encoders))
    def withMetadataDecoders(decoders: Metadata.Decoder.Compiler): Builder[F, Request, Response] =
      copy(metadataDecoders = Some(decoders))
    def withRawStringsAndBlobsPayloads: Builder[F, Request, Response] =
      copy(rawStringsAndBlobPayloads = true)
    def withWriteEmptyStructs(cond: Schema[_] => Boolean): Builder[F, Request, Response] =
      copy(writeEmptyStructs = cond)
    def withResponseMediaType(mediaType: String): Builder[F, Request, Response] =
      copy(responseMediaType = mediaType)
    def withErrorTypeHeaders(headerNames: String*): Builder[F, Request, Response] =
      copy(errorTypeHeaders = headerNames.toList)

    def withRequestTransformation[Request0](f: Request0 => F[Request]): Builder[F, Request0, Response] =
      copy(requestTransformation = f.andThen(F.flatMap(_)(requestTransformation)))
    def withResponseTransformation[Response1](f: Response => F[Response1]): Builder[F, Request, Response1] =
      copy(responseTransformation = responseTransformation.andThen(F.flatMap(_)(f)))

    def build(): UnaryServerCodecs.Make[F, Request, Response] = {
      val setBody: HttpResponse.Encoder[Blob, Blob] = Writer.lift((res, blob) => res.copy(body = blob))
      val setBodyK = Writer.pipeDataK[HttpResponse[Blob], Blob](setBody)

      val mediaTypeWriters = new CachedSchemaCompiler.Uncached[HttpResponse.Encoder[Blob, *]] {
        def fromSchema[A](schema: Schema[A]): HttpResponse.Encoder[Blob, A] = {
          val mt = if (rawStringsAndBlobPayloads) {
            HttpMediaType.fromSchema(schema).map(_.value).getOrElse(responseMediaType)
          } else responseMediaType
          new HttpResponse.Encoder[Blob, A] {
            def write(request: HttpResponse[Blob], value: A): HttpResponse[Blob] =
              if (request.body.isEmpty) request
              else request.withContentType(mt)
          }
        }
      }

      def responseEncoders(blobEncoders: BlobEncoder.Compiler) = {
        val httpBodyWriters: CachedSchemaCompiler[HttpResponse.Encoder[Blob, *]] = if (rawStringsAndBlobPayloads) {
          val finalBodyEncoders = CachedSchemaCompiler
            .getOrElse(smithy4s.codecs.StringAndBlobCodecs.writers, successResponseBodyEncoders)
          finalBodyEncoders.mapK(setBodyK)
        } else successResponseBodyEncoders.mapK(setBodyK)

        val httpMediaWriters: CachedSchemaCompiler[HttpResponse.Encoder[Blob, *]] =
          Writer.combineCompilers(httpBodyWriters, mediaTypeWriters)

        metadataEncoders match {
          case Some(mEncoders) =>
            HttpResponse.Encoder.restSchemaCompiler(mEncoders, httpMediaWriters, writeEmptyStructs)
          case None => httpMediaWriters
        }
      }

      val inputDecoders: CachedSchemaCompiler[HttpRequest.Decoder[F, Blob, *]] = {
        val httpBodyDecoders: CachedSchemaCompiler[Reader[F, Blob, *]] = {
          val decoders: BlobDecoder.Compiler = if (rawStringsAndBlobPayloads) {
            CachedSchemaCompiler
              .getOrElse(smithy4s.codecs.StringAndBlobCodecs.readers, requestBodyDecoders)
          } else requestBodyDecoders
          decoders.mapK(
            Reader
              .of[Blob]
              .liftPolyFunction(
                MonadThrowLike
                  .liftEitherK[F, PayloadError]
                  .andThen(HttpContractError.fromPayloadErrorK[F])
              )
          )
        }

        metadataDecoders match {
          case Some(mDecoders) => HttpRequest.Decoder.restSchemaCompiler(mDecoders, httpBodyDecoders, None)
          case None            => httpBodyDecoders.mapK(HttpRequest.extractBody[F, Blob])
        }
      }

      val outputEncoders = responseEncoders(successResponseBodyEncoders)
      val errorEncoders = responseEncoders(errorResponseBodyEncoders)
      val httpContractErrorWriters = errorEncoders.fromSchema(HttpContractError.schema)

      new UnaryServerCodecs.Make[F, Request, Response] {

        private val inputDecoderCache: inputDecoders.Cache = inputDecoders.createCache()
        private val outputEncoderCache: outputEncoders.Cache = outputEncoders.createCache()

        def apply[I, E, O, SI, SO](
            endpoint: OperationSchema[I, E, O, SI, SO]
        ): UnaryServerCodecs[F, Request, Response, I, E, O] = {
          val outputW = endpoint.hints.get(smithy.api.Http) match {
            case Some(http) =>
              val preProcess: HttpResponse[Blob] => HttpResponse[Blob] =
                _.withStatusCode(http.code)
              // TODO : add unit-tests for this
              val postProcessResponse: HttpResponse[Blob] => HttpResponse[Blob] =
                if (http.code == 204 || http.method.value.toLowerCase == "head")
                  _.withBody(Blob.empty)
                else identity
              outputEncoders
                .fromSchema(endpoint.output, outputEncoderCache)
                .compose[HttpResponse[Blob]](preProcess)
                .andThen[HttpResponse[Blob]](postProcessResponse)
            case None => outputEncoders.fromSchema(endpoint.output, outputEncoderCache)
          }
          val errorW = HttpResponse.Encoder.forError(errorTypeHeaders, endpoint.error, errorEncoders)
          val inputDecoder: HttpRequest.Decoder[F, Blob, I] =
            inputDecoders.fromSchema(endpoint.input, inputDecoderCache)
          val base = baseResponse(endpoint)

          def encodeOutput(o: O) = F.map(base)(outputW.write(_, o))
          def encodeError(e: E) = F.map(base)(errorW.write(_, e))
          def httpContractErrorEncoder(e: HttpContractError) =
            F.map(base)(httpContractErrorWriters.write(_, e).withStatusCode(400))
          def throwableEncoders(throwable: Throwable): F[HttpResponse[Blob]] =
            throwable match {
              case e: HttpContractError => httpContractErrorEncoder(e)
              case e                    => F.raiseError(e)
            }

          new UnaryServerCodecs(inputDecoder.read, encodeError, throwableEncoders, encodeOutput)
            .transformRequest(requestTransformation)
            .transformResponse(responseTransformation)
        }

      }
    }
  }

}
