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
import smithy4s.http.HttpRestSchema
import smithy4s.http.Metadata
import smithy4s.http.uri.HostEndpoint
import smithy4s.kinds.FunctorK
import smithy4s.kinds.PolyFunction
import smithy4s.schema._

object RequestEncoder {

  def metadataRequestEncoder[F[_]: Concurrent]: RequestEncoder[F, Metadata] =
    new RequestEncoder[F, Metadata] {
      def write(request: Request[F], metadata: Metadata): Request[F] = {
        val uri = request.uri.withMultiValueQueryParams(metadata.query)
        val headers = toHeaders(metadata.headers)
        request.withUri(uri).withHeaders(request.headers ++ headers)
      }
    }

  def fromMetadataEncoder[F[_]: Concurrent, A](
      metadataEncoder: Metadata.Encoder[A]
  ): RequestEncoder[F, A] =
    metadataRequestEncoder[F].contramap(metadataEncoder.encode)

  def fromMetadataEncoderK[F[_]: Concurrent]
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

  def fromHostEndpoint[F[_]: Concurrent, I](
      hostEndpoint: HostEndpoint[I]
  ): RequestEncoder[F, I] = new RequestEncoder[F, I] {
    def write(request: Request[F], input: I): Request[F] = {
      val hostPrefix = hostEndpoint.hostPrefix(input)
      val oldUri = request.uri
      val newAuth = prefixHost(oldUri, hostPrefix)
      val newUri = oldUri.copy(authority = newAuth)
      request.withUri(newUri)
    }
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
    FunctorK[CachedSchemaCompiler].mapK(
      entityEncoderCompiler,
      fromEntityEncoderK[F]
    )

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
    val bodyCompiler = FunctorK[CachedSchemaCompiler].mapK(
      entityEncoderCompiler,
      fromEntityEncoderK
    )
    val metadataCompiler = FunctorK[CachedSchemaCompiler].mapK(
      metadataEncoderCompiler,
      fromMetadataEncoderK
    )
    HttpRestSchema.combineWriterCompilers(metadataCompiler, bodyCompiler)
  }

  def prefixHost(u: Uri, prefix: List[String]): Option[Authority] =
    u.authority.map {
      case auth @ Authority(_, Uri.RegName(hostName), _) =>
        auth.copy(host =
          Uri.RegName(
            hostName.transform(value => s"${prefix.mkString(".")}.$value")
          )
        )
      case other => other
    }

}
