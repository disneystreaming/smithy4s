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

package smithy4s.http

import smithy4s.Errorable
import smithy4s.codecs.{Reader, Writer}
import smithy4s.kinds.PolyFunction
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Alt
import smithy4s.schema.Schema
import smithy4s.capability.MonadThrowLike
import smithy4s.Blob

final case class HttpResponse[+A](
    statusCode: Int,
    headers: Map[CaseInsensitive, Seq[String]],
    body: A
) {
  def withStatusCode(statusCode: Int): HttpResponse[A] =
    this.copy(statusCode = statusCode)
  def withHeaders(headers: Map[CaseInsensitive, Seq[String]]): HttpResponse[A] =
    this.copy(headers = headers)

  def withBody[A0](body: A0)(implicit ev: A <:< A0): HttpResponse[A0] =
    this.copy(body = body)

  def addHeaders(
      headers: Iterable[(CaseInsensitive, Seq[String])]
  ): HttpResponse[A] =
    this.copy(headers = this.headers ++ headers)

  def addHeader(headerName: String, headerValue: String): HttpResponse[A] = {
    val key = CaseInsensitive(headerName)
    headers.get(key) match {
      case Some(values) =>
        copy(headers = headers + (key -> (values :+ headerValue)))
      case None =>
        copy(headers = headers + (key -> Seq(headerValue)))
    }
  }

  def toMetadata: Metadata = Metadata(
    headers = headers,
    statusCode = Some(statusCode)
  )

  def withContentType(contentType: String): HttpResponse[A] =
    this.copy(headers =
      this.headers + (CaseInsensitive("Content-Type") -> Seq(contentType))
    )

  def isSuccessful: Boolean =
    HttpResponse.isStatusCodeSuccess(statusCode)
}

object HttpResponse {
  private[http] type Encoder[Body, A] =
    Writer[HttpResponse[Body], HttpResponse[Body], A]
  private[http] type Decoder[F[_], Body, A] = Reader[F, HttpResponse[Body], A]

  private[http] def isStatusCodeSuccess(statusCode: Int): Boolean =
    100 <= statusCode && statusCode <= 399

  private[http] object Encoder {

    private[http] def fromHttpEndpoint[Body, I](
        httpEndpoint: HttpEndpoint[I]
    ): Encoder[Body, I] = new Encoder[Body, I] {
      def write(response: HttpResponse[Body], input: I): HttpResponse[Body] = {
        response.copy(statusCode = httpEndpoint.code)
      }
    }

    private[http] def restSchemaCompiler[Body](
        metadataEncoders: CachedSchemaCompiler[Metadata.Encoder],
        bodyEncoders: CachedSchemaCompiler[Encoder[Body, *]],
        writeEmptyStructs: Schema[_] => Boolean
    ): CachedSchemaCompiler[Encoder[Body, *]] = {
      val metadataCompiler =
        metadataEncoders.mapK(fromMetadataEncoderK[Body])
      HttpRestSchema.combineWriterCompilers(
        metadataCompiler,
        bodyEncoders,
        writeEmptyStructs
      )
    }

    private[http] def forError[Body, E](
        errorTypeHeaders: List[String],
        maybeErrorable: Option[Errorable[E]],
        encoderCompiler: CachedSchemaCompiler[Encoder[Body, *]]
    ): Encoder[Body, E] = maybeErrorable match {
      case Some(errorable) =>
        forErrorAux(errorTypeHeaders, errorable, encoderCompiler)
      case None => Writer.noop
    }

    private def metadataEncoder[Body]: Encoder[Body, Metadata] = {
      (resp: HttpResponse[Body], meta: Metadata) =>
        val statusCode =
          meta.statusCode
            .filter(isStatusCodeSuccess)
            .getOrElse(resp.statusCode)
        resp
          .withStatusCode(statusCode)
          .addHeaders(meta.headers)
    }

    private def fromMetadataEncoderK[Body]
        : PolyFunction[Metadata.Encoder, Encoder[Body, *]] =
      Metadata.Encoder.toWriterK
        .widen[Writer[HttpResponse[Body], Metadata, *]]
        .andThen(Writer.pipeDataK(metadataEncoder[Body]))

    private def forErrorAux[Body, E](
        errorTypeHeaders: List[String],
        errorable: Errorable[E],
        encoderCompiler: CachedSchemaCompiler[Encoder[Body, *]]
    ): Encoder[Body, E] = {
      val errorUnionSchema = errorable.error
      val dispatcher =
        Alt.Dispatcher(errorUnionSchema.alternatives, errorUnionSchema.ordinal)
      val precompiler = new Alt.Precompiler[Encoder[Body, *]] {
        def apply[Err](
            label: String,
            errorSchema: Schema[Err]
        ): Encoder[Body, Err] = new Encoder[Body, Err] {
          val errorEncoder =
            encoderCompiler.fromSchema(
              errorSchema,
              encoderCompiler.createCache()
            )
          def write(
              response: HttpResponse[Body],
              err: Err
          ): HttpResponse[Body] = {
            val errorCode =
              HttpStatusCode.fromSchema(errorSchema).code(err, 500)
            val encodedResponse = errorEncoder.write(response, err)
            val additionalHeaders = errorTypeHeaders
              .map(h => CaseInsensitive(h) -> Seq(label))
              .toMap
            encodedResponse
              .copy(statusCode = errorCode)
              .addHeaders(additionalHeaders)
          }
        }
      }
      dispatcher.compile(precompiler)
    }

  }

  private[http] object Decoder {

    def restSchemaCompiler[F[_]: MonadThrowLike, Body](
        metadataDecoderCompiler: CachedSchemaCompiler[Metadata.Decoder],
        bodyDecoderCompiler: CachedSchemaCompiler[Reader[F, Body, *]],
        drainBody: Option[HttpResponse[Body] => F[Unit]]
    ): CachedSchemaCompiler[Decoder[F, Body, *]] =
      restSchemaCompilerAux(
        metadataDecoderCompiler,
        bodyDecoderCompiler.mapK { extractBody[F, Body] },
        drainBody.getOrElse(_ => MonadThrowLike[F].pure(()))
      )

    private[smithy4s] def restSchemaCompilerAux[F[_]: MonadThrowLike, Body](
        metadataDecoderCompiler: CachedSchemaCompiler[Metadata.Decoder],
        responseReaders: CachedSchemaCompiler[Decoder[F, Body, *]],
        drainBody: HttpResponse[Body] => F[Unit]
    ): CachedSchemaCompiler[Decoder[F, Body, *]] = {
      val restMetadataCompiler: CachedSchemaCompiler[Decoder[F, Body, *]] =
        metadataDecoderCompiler.mapK(
          Metadata.Decoder.toReaderK.andThen(
            extractMetadata[F](MonadThrowLike.liftEitherK[F, MetadataError])
          )
        )

      HttpRestSchema.combineReaderCompilers[F, HttpResponse[Body]](
        restMetadataCompiler,
        responseReaders,
        drainBody
      )
    }

    /**
    * Creates a response decoder that dispatches the response to
    * the correct alternative, based on some discriminator.
    */
    private[http] def forError[F[_]: MonadThrowLike, Body, E](
        maybeErrorable: Option[Errorable[E]],
        decoderCompiler: CachedSchemaCompiler[Decoder[F, Body, *]],
        discriminate: HttpResponse[Body] => F[HttpDiscriminator],
        toStrict: Body => F[(Body, Blob)]
    ): Decoder[F, Body, E] =
      discriminating(
        discriminate,
        HttpErrorSelector(maybeErrorable, decoderCompiler),
        toStrict
      )

    /**
    * Creates a response decoder that dispatches the response to
    * the correct alternative, based on some discriminator, and
    * then upcasts the error as a throwable
    */
    private[http] def forErrorAsThrowable[F[_]: MonadThrowLike, Body, E](
        maybeErrorable: Option[Errorable[E]],
        decoderCompiler: CachedSchemaCompiler[Decoder[F, Body, *]],
        discriminate: HttpResponse[Body] => F[HttpDiscriminator],
        toStrict: Body => F[(Body, Blob)]
    ): Decoder[F, Body, Throwable] =
      discriminating(
        discriminate,
        HttpErrorSelector.asThrowable(maybeErrorable, decoderCompiler),
        toStrict
      )

    /**
    * Creates a response decoder that dispatches  the response
    * to a given decoder, based on some discriminator.
    */
    private def discriminating[F[_], Body, Discriminator, E](
        discriminate: HttpResponse[Body] => F[Discriminator],
        select: Discriminator => Option[Decoder[F, Body, E]],
        toStrict: Body => F[(Body, Blob)]
    )(implicit F: MonadThrowLike[F]): Decoder[F, Body, E] =
      new Decoder[F, Body, E] {
        def read(response: HttpResponse[Body]): F[E] = {
          F.flatMap(toStrict(response.body)) { case (strictBody, bodyBlob) =>
            val strictResponse = response.copy(body = strictBody)
            F.flatMap(discriminate(strictResponse)) { discriminator =>
              select(discriminator) match {
                case Some(decoder) => decoder.read(strictResponse)
                case None =>
                  F.raiseError(
                    smithy4s.http.UnknownErrorResponse(
                      response.statusCode,
                      response.headers,
                      bodyBlob.toUTF8String
                    )
                  )
              }
            }
          }
        }
      }

  }

  private def extractMetadata[F[_]](
      liftToF: PolyFunction[Either[MetadataError, *], F]
  ): PolyFunction[Metadata.Reader, Decoder[F, Any, *]] =
    Reader
      .in[Either[MetadataError, *]]
      .composeK((_: HttpResponse[Any]).toMetadata)
      .andThen(Reader.liftPolyFunction(liftToF))

  private[smithy4s] def extractBody[F[_], Body]
      : PolyFunction[Reader[F, Body, *], Decoder[F, Body, *]] =
    Reader.in[F].composeK(_.body)

}
