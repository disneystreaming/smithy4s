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

import smithy4s.kinds._
import smithy4s.codecs._
import smithy4s.capability.Covariant
import smithy4s.schema._
import smithy4s.capability.MonadThrowLike
import smithy4s.http.HttpMethod

final case class HttpRequest[+A](
    method: HttpMethod,
    uri: HttpUri,
    headers: Map[CaseInsensitive, Seq[String]],
    body: A
) {
  def map[B](f: A => B): HttpRequest[B] =
    HttpRequest(method, uri, headers, f(body))

  def toMetadata: Metadata = Metadata(
    path = uri.pathParams.getOrElse(Map.empty),
    query = uri.queryParams,
    headers = headers
  )

  def addHeaders(headers: Map[CaseInsensitive, Seq[String]]): HttpRequest[A] =
    this.copy(headers = this.headers ++ headers)

  def withContentType(contentType: String): HttpRequest[A] =
    this.copy(headers =
      this.headers + (CaseInsensitive("Content-Type") -> Seq(contentType))
    )
}

object HttpRequest {
  private[http] type Encoder[Body, A] =
    Writer[HttpRequest[Body], HttpRequest[Body], A]
  private[http] type Decoder[F[_], Body, A] = Reader[F, HttpRequest[Body], A]

  implicit val reqCovariant: Covariant[HttpRequest] =
    new Covariant[HttpRequest] {
      def map[A, B](req: HttpRequest[A])(f: A => B): HttpRequest[B] = req.map(f)
    }

  private[http] object Encoder {

    def restSchemaCompiler[Body](
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

    def fromHttpEndpoint[Body, I](
        httpEndpoint: HttpEndpoint[I]
    ): Encoder[Body, I] = new Encoder[Body, I] {
      def write(request: HttpRequest[Body], input: I): HttpRequest[Body] = {
        val path = httpEndpoint.path(input)
        val staticQueries = httpEndpoint.staticQueryParams
        val oldUri = request.uri
        val newUri =
          oldUri.copy(path = oldUri.path ++ path, queryParams = staticQueries)
        val method = httpEndpoint.method
        request.copy(method = method, uri = newUri)
      }
    }

    private def metadataEncoder[Body]: Encoder[Body, Metadata] = {
      (req: HttpRequest[Body], meta: Metadata) =>
        val oldUri = req.uri
        val newUri =
          oldUri.copy(queryParams = oldUri.queryParams ++ meta.query)
        req.addHeaders(meta.headers).copy(uri = newUri)
    }

    private def fromMetadataEncoderK[Body]
        : PolyFunction[Metadata.Encoder, Encoder[Body, *]] =
      Metadata.Encoder.toWriterK
        .widen[Writer[HttpRequest[Body], Metadata, *]]
        .andThen(Writer.pipeDataK(metadataEncoder[Body]))

  }

  object Decoder {

    def restSchemaCompiler[F[_]: MonadThrowLike, Body](
        metadataDecoders: CachedSchemaCompiler[Metadata.Decoder],
        bodyReaders: CachedSchemaCompiler[Reader[F, Body, *]],
        drainBody: Option[HttpRequest[Body] => F[Unit]]
    ): CachedSchemaCompiler[Decoder[F, Body, *]] = {
      restSchemaCompilerAux(
        metadataDecoders,
        bodyReaders.mapK { extractBody[F, Body] },
        drainBody.getOrElse(_ => MonadThrowLike[F].pure(()))
      )
    }

    private[smithy4s] def restSchemaCompilerAux[F[_]: MonadThrowLike, Body](
        metadataDecoders: CachedSchemaCompiler[Metadata.Decoder],
        responseDecoderCompiler: CachedSchemaCompiler[Decoder[F, Body, *]],
        drainBody: HttpRequest[Body] => F[Unit]
    ): CachedSchemaCompiler[Decoder[F, Body, *]] = {
      val restMetadataCompiler: CachedSchemaCompiler[Decoder[F, Body, *]] =
        metadataDecoders.mapK(
          Metadata.Decoder.toReaderK.andThen(
            extractMetadata[F](MonadThrowLike.liftEitherK[F, MetadataError])
          )
        )

      HttpRestSchema.combineReaderCompilers[F, HttpRequest[Body]](
        restMetadataCompiler,
        responseDecoderCompiler,
        drainBody
      )
    }
  }

  private def extractMetadata[F[_]](
      liftToF: PolyFunction[Either[MetadataError, *], F]
  ): PolyFunction[Metadata.Reader, Decoder[F, Any, *]] =
    Reader
      .in[Either[MetadataError, *]]
      .composeK((_: HttpRequest[Any]).toMetadata)
      .andThen(Reader.of[HttpRequest[Any]].liftPolyFunction(liftToF))

  private[smithy4s] def extractBody[F[_], Body]
      : PolyFunction[Reader[F, Body, *], Decoder[F, Body, *]] =
    Reader.in[F].composeK(_.body)

}
