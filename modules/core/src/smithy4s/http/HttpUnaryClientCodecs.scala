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

import smithy4s.client.UnaryClientCodecs
import smithy4s.codecs.{BlobEncoder, BlobDecoder}
import smithy4s.codecs.{Reader, Writer}
import smithy4s.codecs.PayloadError
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.capability.MonadThrowLike

// scalafmt: { maxColumn = 120 }
object HttpUnaryClientCodecs {

  def builder[F[_]](implicit F: MonadThrowLike[F]): Builder[F, HttpRequest[Blob], HttpResponse[Blob]] =
    HttpUnaryClientCodecsBuilderImpl[F, HttpRequest[Blob], HttpResponse[Blob]](
      baseRequest = _ => F.raiseError(new Exception("Undefined base request")),
      requestBodyEncoders = BlobEncoder.noop,
      successResponseBodyDecoders = BlobDecoder.noop,
      errorResponseBodyDecoders = BlobDecoder.noop,
      errorDiscriminator = _ => F.pure(HttpDiscriminator.Undetermined),
      metadataEncoders = None,
      metadataDecoders = None,
      rawStringsAndBlobPayloads = false,
      writeEmptyStructs = false,
      requestMediaType = "text/plain",
      requestTransformation = F.pure(_),
      responseTransformation = F.pure(_)
    )

  trait Builder[F[_], Request, Response] {
    def withBaseRequest(f: Hints => F[HttpRequest[Blob]]): Builder[F, Request, Response]
    def withBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response]
    def withSuccessBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withErrorBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response]
    def withErrorDiscriminator(f: HttpResponse[Blob] => F[HttpDiscriminator]): Builder[F, Request, Response]
    def withMetadataEncoders(encoders: Metadata.Encoder.Compiler): Builder[F, Request, Response]
    def withMetadataDecoders(decoders: Metadata.Decoder.Compiler): Builder[F, Request, Response]
    def withRawStringsAndBlobsPayloads: Builder[F, Request, Response]
    def withWriteEmptyStructs: Builder[F, Request, Response]
    def withRequestMediaType(mediaType: String): Builder[F, Request, Response]
    def withRequestTransformation[Request1](f: Request => F[Request1]): Builder[F, Request1, Response]
    def withResponseTransformation[Response0](f: Response0 => F[Response]): Builder[F, Request, Response0]
    def build(): UnaryClientCodecs.Make[F, Request, Response]
  }

  private case class HttpUnaryClientCodecsBuilderImpl[F[_], Request, Response](
      baseRequest: Hints => F[HttpRequest[Blob]],
      requestBodyEncoders: BlobEncoder.Compiler,
      successResponseBodyDecoders: BlobDecoder.Compiler,
      errorResponseBodyDecoders: BlobDecoder.Compiler,
      errorDiscriminator: HttpResponse[Blob] => F[HttpDiscriminator],
      metadataEncoders: Option[Metadata.Encoder.Compiler],
      metadataDecoders: Option[Metadata.Decoder.Compiler],
      rawStringsAndBlobPayloads: Boolean,
      writeEmptyStructs: Boolean,
      requestMediaType: String,
      requestTransformation: HttpRequest[Blob] => F[Request],
      responseTransformation: Response => F[HttpResponse[Blob]]
  )(implicit F: MonadThrowLike[F])
      extends Builder[F, Request, Response] {

    def withBaseRequest(f: Hints => F[HttpRequest[Blob]]): Builder[F, Request, Response] =
      copy(baseRequest = f)
    def withBodyEncoders(encoders: BlobEncoder.Compiler): Builder[F, Request, Response] =
      copy(requestBodyEncoders = encoders)
    def withSuccessBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response] =
      copy(successResponseBodyDecoders = decoders)
    def withErrorBodyDecoders(decoders: BlobDecoder.Compiler): Builder[F, Request, Response] =
      copy(errorResponseBodyDecoders = decoders)
    def withErrorDiscriminator(f: HttpResponse[Blob] => F[HttpDiscriminator]): Builder[F, Request, Response] =
      copy(errorDiscriminator = f)
    def withMetadataEncoders(encoders: Metadata.Encoder.Compiler): Builder[F, Request, Response] =
      copy(metadataEncoders = Some(encoders))
    def withMetadataDecoders(decoders: Metadata.Decoder.Compiler): Builder[F, Request, Response] =
      copy(metadataDecoders = Some(decoders))
    def withRawStringsAndBlobsPayloads: Builder[F, Request, Response] =
      copy(rawStringsAndBlobPayloads = true)
    def withWriteEmptyStructs: Builder[F, Request, Response] =
      copy(writeEmptyStructs = true)
    def withRequestMediaType(mediaType: String): Builder[F, Request, Response] =
      copy(requestMediaType = mediaType)

    def withRequestTransformation[Request1](f: Request => F[Request1]): Builder[F, Request1, Response] =
      copy(requestTransformation = requestTransformation.andThen(F.flatMap(_)(f)))
    def withResponseTransformation[Response0](f: Response0 => F[Response]): Builder[F, Request, Response0] =
      copy(responseTransformation = f.andThen(F.flatMap(_)(responseTransformation)))

    def build(): UnaryClientCodecs.Make[F, Request, Response] = {
      val setBody: HttpRequest.Encoder[Blob, Blob] = Writer.lift((req, blob) => req.copy(body = blob))
      val setBodyK = Writer
        .pipeDataK[HttpRequest[Blob], Blob](setBody)

      val mediaTypeWriters = new CachedSchemaCompiler.Uncached[HttpRequest.Encoder[Blob, *]] {
        def fromSchema[A](schema: Schema[A]): HttpRequest.Encoder[Blob, A] = {
          val mt = if (rawStringsAndBlobPayloads) {
            HttpMediaType.fromSchema(schema).map(_.value).getOrElse(requestMediaType)
          } else requestMediaType
          new HttpRequest.Encoder[Blob, A] {
            def write(request: HttpRequest[Blob], value: A): HttpRequest[Blob] =
              if (request.body.isEmpty) request
              else request.withContentType(mt)
          }
        }
      }

      val httpBodyWriters: CachedSchemaCompiler[HttpRequest.Encoder[Blob, *]] = if (rawStringsAndBlobPayloads) {
        val finalBodyEncoders = CachedSchemaCompiler
          .getOrElse(smithy4s.codecs.StringAndBlobCodecs.writers, requestBodyEncoders)
        finalBodyEncoders.mapK(setBodyK)
      } else requestBodyEncoders.mapK(setBodyK)

      val httpMediaWriter: CachedSchemaCompiler[HttpRequest.Encoder[Blob, *]] =
        Writer.combineCompilers(httpBodyWriters, mediaTypeWriters)

      def responseDecoders(blobDecoders: BlobDecoder.Compiler) = {
        val httpBodyDecoders: CachedSchemaCompiler[Reader[F, Blob, *]] = {
          val decoders: BlobDecoder.Compiler = if (rawStringsAndBlobPayloads) {
            CachedSchemaCompiler
              .getOrElse(smithy4s.codecs.StringAndBlobCodecs.readers, blobDecoders)
          } else blobDecoders
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
          case Some(mDecoders) => HttpResponse.Decoder.restSchemaCompiler(mDecoders, httpBodyDecoders)
          case None            => httpBodyDecoders.mapK(HttpResponse.extractBody[F, Blob])
        }
      }

      val inputEncoders = metadataEncoders match {
        case Some(mEncoders) => HttpRequest.Encoder.restSchemaCompiler(mEncoders, httpMediaWriter)
        case None            => httpMediaWriter
      }
      val outputDecoders = responseDecoders(successResponseBodyDecoders)
      val errorDecoders = responseDecoders(errorResponseBodyDecoders)

      new UnaryClientCodecs.Make[F, Request, Response] {

        private val inputEncoderCache: inputEncoders.Cache = inputEncoders.createCache()
        private val outputDecoderCache: outputDecoders.Cache = outputDecoders.createCache()

        def apply[I, E, O, SI, SO](
            endpoint: Endpoint.Base[I, E, O, SI, SO]
        ): UnaryClientCodecs[F, Request, Response, I, E, O] = {

          val inputWriter: HttpRequest.Encoder[Blob, I] =
            HttpEndpoint.cast(endpoint).toOption match {
              case Some(httpEndpoint) => {
                val httpInputEncoder =
                  HttpRequest.Encoder.fromHttpEndpoint[Blob, I](httpEndpoint)
                val requestEncoder =
                  inputEncoders.fromSchema(endpoint.input, inputEncoderCache)
                httpInputEncoder.pipe(requestEncoder)
              }
              case None => inputEncoders.fromSchema(endpoint.input, inputEncoderCache)
            }

          val inputEncoder = (i: I) => F.map(baseRequest(endpoint.hints))(inputWriter.write(_, i))

          val outputDecoder: HttpResponse.Decoder[F, Blob, O] =
            outputDecoders.fromSchema(endpoint.output, outputDecoderCache)

          def toStrict(blob: Blob): F[(Blob, Blob)] = F.pure((blob, blob))

          val errorDecoder: HttpResponse.Decoder[F, Blob, Throwable] =
            HttpResponse.Decoder.forErrorAsThrowable(
              endpoint.errorable,
              errorDecoders,
              errorDiscriminator,
              toStrict
            )
          new UnaryClientCodecs(inputEncoder, errorDecoder.read, outputDecoder.read)
            .transformRequest[Request](requestTransformation)
            .transformResponse[Response](responseTransformation)
        }

      }
    }
  }

}
