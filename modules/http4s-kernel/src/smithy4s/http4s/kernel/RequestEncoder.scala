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
import smithy4s.PartialData
import smithy4s.capability.Encoder
import smithy4s.http.HttpEndpoint
import smithy4s.http.HttpRestSchema
import smithy4s.http.Metadata
import smithy4s.schema._

object RequestEncoder {

  def fromMetadataEncoder[F[_]: Concurrent, A](
      metadataEncoder: Metadata.Encoder[A]
  ): RequestEncoder[F, A] = new RequestEncoder[F, A] {
    def encode(request: Request[F], a: A): Request[F] = {
      val metadata = metadataEncoder.encode(a)
      val uri = request.uri
        .withMultiValueQueryParams(metadata.query)
      val headers = toHeaders(metadata.headers)
      request.withUri(uri).withHeaders(request.headers ++ headers)
    }
  }

  def fromEntityEncoder[F[_]: Concurrent, A](implicit
      entityEncoder: EntityEncoder[F, A]
  ): RequestEncoder[F, A] = new RequestEncoder[F, A] {
    def encode(request: Request[F], a: A): Request[F] = {
      request.withEntity(a)
    }
  }

  def fromHttpEndpoint[F[_]: Concurrent, I](
      httpEndpoint: HttpEndpoint[I]
  ): RequestEncoder[F, I] = new RequestEncoder[F, I] {
    def encode(request: Request[F], input: I): Request[F] = {
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

  def rpcSchemaCompiler[F[_]](
      entityDecoderCompiler: CachedSchemaCompiler[EntityEncoder[F, *]]
  )(implicit F: Concurrent[F]): CachedSchemaCompiler[RequestEncoder[F, *]] =
    new CachedSchemaCompiler[RequestEncoder[F, *]] {
      type Cache = entityDecoderCompiler.Cache
      def createCache(): Cache =
        entityDecoderCompiler.createCache()

      def fromSchema[A](schema: Schema[A], cache: Cache): RequestEncoder[F, A] =
        fromEntityEncoder(F, entityDecoderCompiler.fromSchema(schema, cache))
      def fromSchema[A](schema: Schema[A]): RequestEncoder[F, A] =
        fromEntityEncoder(F, entityDecoderCompiler.fromSchema(schema))
    }

  /**
    * A compiler for RequestEncoder that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`,
    * `httpStatusCode` ... are encoded as the corresponding metadata.
    *
    * The rest is used to formulate the body of the message.
    */
  def restSchemaCompiler[F[_]](
      entityEncoderCompiler: CachedSchemaCompiler[EntityEncoder[F, *]]
  )(implicit F: Concurrent[F]): CachedSchemaCompiler[RequestEncoder[F, *]] =
    new CachedSchemaCompiler[RequestEncoder[F, *]] {
      type MetadataCache = Metadata.Encoder.Cache
      type EntityCache = entityEncoderCompiler.Cache
      type Cache = (EntityCache, MetadataCache)
      def createCache(): Cache = {
        val eCache = entityEncoderCompiler.createCache()
        val mCache = Metadata.Encoder.createCache()
        (eCache, mCache)
      }
      def fromSchema[A](schema: Schema[A]): RequestEncoder[F, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          fullSchema: Schema[A],
          cache: Cache
      ): RequestEncoder[F, A] = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata.
            val metadataEncoder =
              Metadata.Encoder.fromSchema(metadataSchema, cache._2)
            RequestEncoder.fromMetadataEncoder(metadataEncoder)
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            implicit val bodyDecoder: EntityEncoder[F, A] =
              entityEncoderCompiler.fromSchema(bodySchema, cache._1)
            RequestEncoder.fromEntityEncoder(F, bodyDecoder)
          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataEncoder =
              Metadata.Encoder.fromSchema(metadataSchema, cache._2)
            val metadataRequestEncoder =
              RequestEncoder
                .fromMetadataEncoder(metadataEncoder)
                .contramap[A](PartialData.Total(_))
            implicit val bodyEncoder: EntityEncoder[F, A] =
              entityEncoderCompiler
                .fromSchema(bodySchema, cache._1)
                .contramap[A](PartialData.Total(_))
            val bodyRequestEncoder =
              RequestEncoder
                .fromEntityEncoder(F, bodyEncoder)
            metadataRequestEncoder.combine(bodyRequestEncoder)
          case HttpRestSchema.Empty(_) =>
            Encoder.noop
          // format: on
        }
      }
    }

}
