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

import cats.effect.Concurrent
import org.http4s.EntityEncoder
import org.http4s.Method
import org.http4s.Request
import org.http4s.Uri
import smithy4s.http.HttpEndpoint
import smithy4s.http.Metadata
import smithy4s.kinds.PolyFunction
import smithy4s.schema._
import smithy4s.http.HttpRestSchema

object RequestEncoder {

  type CachedCompiler[F[_]] = CachedSchemaCompiler[RequestEncoder[F, *]]

  def metadataRequestEncoder[F[_]]: RequestEncoder[F, Metadata] =
    new RequestEncoder[F, Metadata] {
      def write(request: Request[F], metadata: Metadata): Request[F] = {
        val uri = request.uri.withMultiValueQueryParams(metadata.query)
        val headers = toHeaders(metadata.headers)
        request.withUri(uri).withHeaders(request.headers ++ headers)
      }
    }

  def fromMetadataEncoder[F[_], A](
      metadataEncoder: Metadata.Encoder[A]
  ): RequestEncoder[F, A] =
    metadataRequestEncoder[F].contramap(metadataEncoder.encode)

  def fromMetadataEncoderK[F[_]]
      : PolyFunction[Metadata.Encoder, RequestEncoder[F, *]] =
    new PolyFunction[Metadata.Encoder, RequestEncoder[F, *]] {
      def apply[A](fa: Metadata.Encoder[A]): RequestEncoder[F, A] =
        fromMetadataEncoder[F, A](fa)
    }

  def fromEntityEncoder[F[_]: Concurrent, A](implicit
      entityEncoder: EntityEncoder[F, A]
  ): RequestEncoder[F, A] = new RequestEncoder[F, A] {
    def write(request: Request[F], a: A): Request[F] = {
      request.withEntity(a)
    }
  }

  def fromEntityEncoderK[F[_]: Concurrent]
      : PolyFunction[EntityEncoder[F, *], RequestEncoder[F, *]] =
    new PolyFunction[EntityEncoder[F, *], RequestEncoder[F, *]] {
      def apply[A](fa: EntityEncoder[F, A]): RequestEncoder[F, A] =
        fromEntityEncoder[F, A](Concurrent[F], fa)
    }

  def fromHttpEndpoint[F[_]: Concurrent, I](
      httpEndpoint: HttpEndpoint[I]
  ): RequestEncoder[F, I] = new RequestEncoder[F, I] {
    def write(request: Request[F], input: I): Request[F] = {
      val path = httpEndpoint.path(input)
      val staticQueries = httpEndpoint.staticQueryParams
      val oldUri = request.uri
      val newUri = oldUri
        .copy(path = oldUri.path.addSegments(path.map(Uri.Path.Segment(_))))
        .withMultiValueQueryParams(staticQueries)
      val method = toHttp4sMethod(httpEndpoint.method).getOrElse(Method.POST)
      request.withUri(newUri).withMethod(method)
    }
  }

  /**
    * A compiler for RequestEncoder that encodes the whole data in the body
    * of the request
    */
  def rpcSchemaCompiler[F[_]](
      entityEncoderCompiler: CachedSchemaCompiler[EntityEncoder[F, *]]
  )(implicit F: Concurrent[F]): CachedSchemaCompiler[RequestEncoder[F, *]] =
    entityEncoderCompiler.mapK(fromEntityEncoderK[F])

  /**
    * A compiler for RequestEncoder that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`
    * ... are encoded as the corresponding metadata.
    *
    * The rest is used to formulate the body of the message.
    */
  def restSchemaCompiler[F[_]](
      metadataEncoderCompiler: CachedSchemaCompiler[Metadata.Encoder],
      entityEncoderCompiler: CachedSchemaCompiler[EntityEncoder[F, *]]
  )(implicit F: Concurrent[F]): CachedSchemaCompiler[RequestEncoder[F, *]] = {
    val bodyCompiler = entityEncoderCompiler.mapK(fromEntityEncoderK)
    val metadataCompiler = metadataEncoderCompiler.mapK(fromMetadataEncoderK[F])
    HttpRestSchema.combineWriterCompilers(metadataCompiler, bodyCompiler)
  }

}
