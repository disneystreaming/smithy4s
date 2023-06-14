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
import org.http4s.Request
import smithy4s.http.Metadata
import smithy4s.schema._
import smithy4s.kinds.FunctorK
import smithy4s.kinds.PolyFunction

object RequestDecoder {

  def fromEntityDecoder[F[_], A](implicit
      F: MonadThrow[F],
      entityDecoder: EntityDecoder[F, A]
  ): RequestDecoder[F, A] = new RequestDecoder[F, A] {
    def decode(request: Request[F]): F[A] = request.as[A]
  }

  /**
    * Creates a RequestDecoder that decodes an HTTP message by looking at the
    * metadata.
    *
    * NB: This decoder assumes that incoming requests have been enriched with pre-extracted
    * path-parameters in the vault.
    */
  def fromMetadataDecoder[F[_]: MonadThrow, A](
      metadataDecoder: Metadata.Decoder[A]
  ): RequestDecoder[F, A] = new RequestDecoder[F, A] {
    def decode(request: Request[F]): F[A] = {
      // TODO better recovery when the pathParams cannot be retrieved from the vault
      val queryParams =
        request.attributes.lookup(pathParamsKey).getOrElse(Map.empty)
      val metadata = getRequestMetadata(queryParams, request)
      MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
    }
  }

  def fromMetadataDecoderK[F[_]: MonadThrow]
      : PolyFunction[Metadata.Decoder, RequestDecoder[F, *]] =
    new PolyFunction[Metadata.Decoder, RequestDecoder[F, *]] {
      def apply[A](fa: Metadata.Decoder[A]): RequestDecoder[F, A] =
        fromMetadataDecoder(fa)
    }

  def rpcSchemaCompiler[F[_]](
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  )(implicit F: MonadThrow[F]): CachedSchemaCompiler[RequestDecoder[F, *]] =
    new CachedSchemaCompiler[RequestDecoder[F, *]] {
      type Cache = entityDecoderCompiler.Cache
      def createCache(): Cache =
        entityDecoderCompiler.createCache()

      def fromSchema[A](schema: Schema[A], cache: Cache): RequestDecoder[F, A] =
        fromEntityDecoder(F, entityDecoderCompiler.fromSchema(schema, cache))
      def fromSchema[A](schema: Schema[A]): RequestDecoder[F, A] =
        fromEntityDecoder(F, entityDecoderCompiler.fromSchema(schema))
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
  ): CachedSchemaCompiler[RequestDecoder[F, *]] = {
    val metadataCompiler = FunctorK[CachedSchemaCompiler]
      .mapK(metadataDecoderCompiler, fromMetadataDecoderK[F])
    val bodyCompiler = FunctorK[CachedSchemaCompiler].mapK(
      entityDecoderCompiler,
      MessageDecoder.fromEntityDecoderK
    )
    MessageDecoder.restCombinedSchemaCompiler[F, Request[F]](
      metadataCompiler,
      bodyCompiler
    )
  }
}
