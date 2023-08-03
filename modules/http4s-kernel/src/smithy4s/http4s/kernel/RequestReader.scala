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

package smithy4s.http4s.kernel

import cats.MonadThrow
import cats.effect.Concurrent
import org.http4s.EntityDecoder
import org.http4s.Media
import org.http4s.Request
import smithy4s.codecs.Reader
import smithy4s.http.HttpRequest
import smithy4s.http.HttpUri
import smithy4s.http.Metadata
import smithy4s.http.MetadataError
import smithy4s.kinds.PolyFunction
import smithy4s.schema._

object RequestReader {

  type CachedCompiler[F[_]] = CachedSchemaCompiler[RequestReader[F, *]]

  private def liftEither[F[_]: MonadThrow]
      : PolyFunction[Either[MetadataError, *], F] =
    new PolyFunction[Either[MetadataError, *], F] {
      def apply[A](fa: Either[MetadataError, A]): F[A] =
        MonadThrow[F].fromEither(fa)
    }

  private def toHttpRequest[F[_]](req: Request[F]): HttpRequest[Media[F]] = {
    val pathParams = req.attributes.lookup(pathParamsKey)
    val params = getQueryParams(req)
    // EXTRACT the host
    val uri = HttpUri("localhost", req.uri.path.segments.map(_.encoded), params)
    val headers = getHeaders(req)
    HttpRequest(uri, headers, req, pathParams)
  }

  private def fromHttpRequest[F[_]]: PolyFunction[
    HttpRequest.HttpRequestReader[F, Media[F], *],
    RequestReader[F, *]
  ] = new PolyFunction[
    HttpRequest.HttpRequestReader[F, Media[F], *],
    RequestReader[F, *]
  ]() {
    def apply[A](
        fa: Reader[F, HttpRequest[Media[F]], A]
    ): RequestReader[F, A] = {
      fa.compose[Request[F]](toHttpRequest)
    }
  }

  /**
    * A compiler for RequestDecoder that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`,
    * `httpStatusCode` ... are decoded from the corresponding metadata.
    *
    * The rest is decoded from the body.
    */
  def restSchemaCompiler[F[_]](
      metadataDecoderCompiler: CachedSchemaCompiler[Metadata.Decoder],
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  )(implicit
      F: Concurrent[F]
  ): CachedSchemaCompiler[RequestReader[F, *]] = {
    val bodyCompiler =
      entityDecoderCompiler.mapK(MediaDecoder.fromEntityDecoderK)
    val httpRequestCompiler = HttpRequest.restSchemaCompiler[F, Media[F]](
      metadataDecoderCompiler,
      bodyCompiler,
      liftEither
    )
    httpRequestCompiler.mapK(fromHttpRequest[F])
  }
}
