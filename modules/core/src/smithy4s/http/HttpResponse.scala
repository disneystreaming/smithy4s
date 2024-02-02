/*
 *  Copyright 2021-2024 Disney Streaming
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

import smithy4s.Blob
import smithy4s.capability.MonadThrowLike
import smithy4s.codecs.Writer
import smithy4s.codecs.{Decoder => GenericDecoder}
import smithy4s.kinds.PolyFunction
import smithy4s.schema.Alt
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.ErrorSchema
import smithy4s.schema.Schema

final case class HttpResponse[+A] private (
    statusCode: Int,
    headers: Map[CaseInsensitive, Seq[String]],
    body: A
) {
  def withStatusCode(statusCode: Int): HttpResponse[A] =
    this.copy(statusCode = statusCode)
  def withHeaders(headers: Map[CaseInsensitive, Seq[String]]): HttpResponse[A] =
    this.copy(headers = headers)

  def withBody[A0](body: A0): HttpResponse[A0] =
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
  def apply[A](
      statusCode: Int,
      headers: Map[CaseInsensitive, Seq[String]],
      body: A
  ): HttpResponse[A] = {
    new HttpResponse(statusCode, headers, body)
  }
  private[http] type Writer[Body, A] =
    smithy4s.codecs.Writer[HttpResponse[Body], A]
  private[http] type Decoder[F[_], Body, A] =
    smithy4s.codecs.Decoder[F, HttpResponse[Body], A]

  private[http] def isStatusCodeSuccess(statusCode: Int): Boolean =
    100 <= statusCode && statusCode <= 399

  private[http] object Encoder {

    private[http] def fromHttpEndpoint[Body, I](
        httpEndpoint: HttpEndpoint[I]
    ): Writer[Body, I] = new Writer[Body, I] {
      def write(response: HttpResponse[Body], input: I): HttpResponse[Body] = {
        response.copy(statusCode = httpEndpoint.code)
      }
    }

    private[http] def restSchemaCompiler[Body](
        metadataEncoders: CachedSchemaCompiler[Metadata.Encoder],
        bodyEncoders: CachedSchemaCompiler[Writer[Body, *]],
        writeEmptyStructs: Schema[_] => Boolean
    ): CachedSchemaCompiler[Writer[Body, *]] = {
      val metadataCompiler = metadataEncoders.mapK(
        smithy4s.codecs.Encoder.pipeToWriterK(metadataWriter[Body])
      )
      HttpRestSchema.combineWriterCompilers(
        metadataCompiler,
        bodyEncoders,
        writeEmptyStructs
      )
    }

    private[http] def forError[Body, E](
        errorTypeHeaders: List[String],
        maybeErrorable: Option[ErrorSchema[E]],
        encoderCompiler: CachedSchemaCompiler[Writer[Body, *]]
    ): Writer[Body, E] = maybeErrorable match {
      case Some(errorschema) =>
        forErrorAux(errorTypeHeaders, errorschema, encoderCompiler)
      case None => Writer.noop
    }

    private def metadataWriter[Body]: Writer[Body, Metadata] = {
      (resp: HttpResponse[Body], meta: Metadata) =>
        val statusCode =
          meta.statusCode
            .filter(isStatusCodeSuccess)
            .getOrElse(resp.statusCode)
        resp
          .withStatusCode(statusCode)
          .addHeaders(meta.headers)
    }

    private def forErrorAux[Body, E](
        errorTypeHeaders: List[String],
        errorSchema: ErrorSchema[E],
        encoderCompiler: CachedSchemaCompiler[Writer[Body, *]]
    ): Writer[Body, E] = {
      val dispatcher =
        Alt.Dispatcher(errorSchema.alternatives, errorSchema.ordinal)
      val precompiler = new Alt.Precompiler[Writer[Body, *]] {
        def apply[Err](
            label: String,
            errorSchema: Schema[Err]
        ): Writer[Body, Err] = new Writer[Body, Err] {
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
        bodyDecoderCompiler: CachedSchemaCompiler[GenericDecoder[F, Body, *]],
        drainBody: Option[HttpResponse[Body] => F[Unit]]
    ): CachedSchemaCompiler[Decoder[F, Body, *]] =
      restSchemaCompilerAux(
        metadataDecoderCompiler,
        bodyDecoderCompiler.mapK { extractBody[F, Body] },
        drainBody.getOrElse(_ => MonadThrowLike[F].pure(()))
      )

    private[smithy4s] def restSchemaCompilerAux[F[_]: MonadThrowLike, Body](
        metadataDecoderCompiler: CachedSchemaCompiler[Metadata.Decoder],
        responseDecoders: CachedSchemaCompiler[Decoder[F, Body, *]],
        drainBody: HttpResponse[Body] => F[Unit]
    ): CachedSchemaCompiler[Decoder[F, Body, *]] = {
      val restMetadataCompiler: CachedSchemaCompiler[Decoder[F, Body, *]] =
        metadataDecoderCompiler.mapK(
          extractMetadata[F](MonadThrowLike.liftEitherK[F, MetadataError])
        )

      HttpRestSchema.combineDecoderCompilers[F, HttpResponse[Body]](
        restMetadataCompiler,
        responseDecoders,
        drainBody
      )
    }

    /**
    * Creates a response decoder that dispatches the response to
    * the correct alternative, based on some discriminator.
    */
    private[http] def forError[F[_]: MonadThrowLike, Body, E](
        maybeErrorSchema: Option[ErrorSchema[E]],
        decoderCompiler: CachedSchemaCompiler[Decoder[F, Body, *]],
        discriminate: HttpResponse[Body] => F[HttpDiscriminator],
        toStrict: Body => F[(Body, Blob)]
    ): Decoder[F, Body, E] =
      discriminating(
        discriminate,
        HttpErrorSelector(maybeErrorSchema, decoderCompiler),
        toStrict
      )

    /**
    * Creates a response decoder that dispatches the response to
    * the correct alternative, based on some discriminator, and
    * then upcasts the error as a throwable
    */
    private[http] def forErrorAsThrowable[F[_]: MonadThrowLike, Body, E](
        maybeErrorSchema: Option[ErrorSchema[E]],
        decoderCompiler: CachedSchemaCompiler[Decoder[F, Body, *]],
        discriminate: HttpResponse[Body] => F[HttpDiscriminator],
        toStrict: Body => F[(Body, Blob)]
    ): Decoder[F, Body, Throwable] =
      discriminating(
        discriminate,
        HttpErrorSelector.asThrowable(maybeErrorSchema, decoderCompiler),
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
        def decode(response: HttpResponse[Body]): F[E] = {
          F.flatMap(toStrict(response.body)) { case (strictBody, bodyBlob) =>
            val strictResponse = response.copy(body = strictBody)
            F.flatMap(discriminate(strictResponse)) { discriminator =>
              select(discriminator) match {
                case Some(decoder) => decoder.decode(strictResponse)
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
  ): PolyFunction[Metadata.Decoder, Decoder[F, Any, *]] =
    GenericDecoder
      .in[Either[MetadataError, *]]
      .composeK((_: HttpResponse[Any]).toMetadata)
      .andThen(GenericDecoder.of[HttpResponse[Any]].liftPolyFunction(liftToF))

  private[smithy4s] def extractBody[F[_], Body]
      : PolyFunction[GenericDecoder[F, Body, *], Decoder[F, Body, *]] =
    GenericDecoder.in[F].composeK(_.body)

}
