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

import smithy4s.kinds._
import smithy4s.capability.Covariant
import smithy4s.schema._
import smithy4s.codecs.{Decoder => GenericDecoder}
import smithy4s.capability.MonadThrowLike

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
  private[http] type Writer[Body, A] =
    smithy4s.codecs.Writer[HttpRequest[Body], A]
  private[http] type Decoder[F[_], Body, A] =
    smithy4s.codecs.Decoder[F, HttpRequest[Body], A]

  implicit val reqCovariant: Covariant[HttpRequest] =
    new Covariant[HttpRequest] {
      def map[A, B](req: HttpRequest[A])(f: A => B): HttpRequest[B] = req.map(f)
    }

  private[http] object Writer {

    def restSchemaCompiler[Body](
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

    def fromHttpEndpoint[Body, I](
        httpEndpoint: HttpEndpoint[I]
    ): Writer[Body, I] = new Writer[Body, I] {
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

    private def metadataWriter[Body]: Writer[Body, Metadata] = {
      (req: HttpRequest[Body], meta: Metadata) =>
        val oldUri = req.uri
        val newUri =
          oldUri.copy(queryParams = oldUri.queryParams ++ meta.query)
        req.addHeaders(meta.headers).copy(uri = newUri)
    }

    private[http] def hostPrefix[Body, I](
        httpEndpoint: OperationSchema[I, _, _, _, _]
    ): Writer[Body, I] = {
      HttpHostPrefix(httpEndpoint) match {
        case Some(prefixEncoder) =>
          new Writer[Body, I] {
            def write(
                request: HttpRequest[Body],
                input: I
            ): HttpRequest[Body] = {
              val hostPrefix = prefixEncoder.write(List.empty, input)
              val oldUri = request.uri
              val newUri =
                oldUri.copy(host = s"${hostPrefix.mkString}${oldUri.host}")
              request.copy(uri = newUri)
            }
          }
        case None =>
          (r, _) => r
      }
    }

  }

  object Decoder {

    def restSchemaCompiler[F[_]: MonadThrowLike, Body](
        metadataDecoders: CachedSchemaCompiler[Metadata.Decoder],
        bodyDecoders: CachedSchemaCompiler[GenericDecoder[F, Body, *]],
        drainBody: Option[HttpRequest[Body] => F[Unit]]
    ): CachedSchemaCompiler[Decoder[F, Body, *]] = {
      restSchemaCompilerAux(
        metadataDecoders,
        bodyDecoders.mapK { extractBody[F, Body] },
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
          extractMetadata[F](MonadThrowLike.liftEitherK[F, MetadataError])
        )

      HttpRestSchema.combineDecoderCompilers[F, HttpRequest[Body]](
        restMetadataCompiler,
        responseDecoderCompiler,
        drainBody
      )
    }
  }

  private def extractMetadata[F[_]](
      liftToF: PolyFunction[Either[MetadataError, *], F]
  ): PolyFunction[Metadata.Decoder, Decoder[F, Any, *]] =
    GenericDecoder
      .in[Either[MetadataError, *]]
      .composeK((_: HttpRequest[Any]).toMetadata)
      .andThen(GenericDecoder.of[HttpRequest[Any]].liftPolyFunction(liftToF))

  private[smithy4s] def extractBody[F[_], Body]
      : PolyFunction[GenericDecoder[F, Body, *], Decoder[F, Body, *]] =
    GenericDecoder.in[F].composeK(_.body)

}
