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
import cats.syntax.all._
import org.http4s.EntityDecoder
import org.http4s.Request
import smithy4s.PartialData
import smithy4s.http.HttpRestSchema
import smithy4s.http.Metadata
import smithy4s.schema._

trait RequestDecoder[F[_], A] {
  def decodeRequest(request: Request[F]): F[A]
}

object RequestDecoder {

  def fromEntityDecoder[F[_], A](implicit
      F: MonadThrow[F],
      entityDecoder: EntityDecoder[F, A]
  ): RequestDecoder[F, A] = new RequestDecoder[F, A] {
    def decodeRequest(request: Request[F]): F[A] = request.as[A]
  }

  /**
    * Creates a RequestDecoder that decodes an HTTP message by looking at the
    * metadata.
    *
    * NB: This decoder assumes that incoming requests have been enriched with pre-extracted
    * path-parameters in the vault.
    */
  def fromMetadataDecoder[F[_]: Concurrent, A](
      metadataDecoder: Metadata.Decoder[A],
      drainMessage: Boolean
  ): RequestDecoder[F, A] = new RequestDecoder[F, A] {
    def decodeRequest(request: Request[F]): F[A] = {
      // TODO better recovery when the pathParams cannot be retrieved from the vault
      val queryParams =
        request.attributes.lookup(pathParamsKey).getOrElse(Map.empty)
      val metadata = getRequestMetadata(queryParams, request)
      val drain = request.body.compile.drain.whenA(drainMessage)
      val decode = MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
      decode <* drain
    }
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
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  )(implicit
      F: Concurrent[F]
  ): CachedSchemaCompiler[RequestDecoder[F, *]] =
    new CachedSchemaCompiler[RequestDecoder[F, *]] {
      type MetadataCache = Metadata.Decoder.Cache
      type EntityCache = entityDecoderCompiler.Cache
      type Cache = (EntityCache, MetadataCache)
      def createCache(): Cache = {
        val eCache = entityDecoderCompiler.createCache()
        val mCache = Metadata.Decoder.createCache()
        (eCache, mCache)
      }
      def fromSchema[A](schema: Schema[A]): RequestDecoder[F, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          fullSchema: Schema[A],
          cache: Cache
      ): RequestDecoder[F, A] = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata.
            val metadataDecoder =
              Metadata.Decoder.fromSchema(metadataSchema, cache._2)
            RequestDecoder.fromMetadataDecoder(
              metadataDecoder,
              drainMessage = true
            )
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            implicit val bodyDecoder: EntityDecoder[F, A] =
              entityDecoderCompiler.fromSchema(bodySchema, cache._1)
            RequestDecoder.fromEntityDecoder(F, bodyDecoder)

          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataDecoder =
              Metadata.Decoder.fromSchema(metadataSchema, cache._2)
            val metadataRequestDecoder =
              RequestDecoder.fromMetadataDecoder[F, PartialData[A]](
                metadataDecoder,
                drainMessage = false
              )
            implicit val bodyDecoder: EntityDecoder[F, PartialData[A]] =
              entityDecoderCompiler.fromSchema(bodySchema, cache._1)

            // format: off
            new RequestDecoder[F, A] {
              def decodeRequest(request: Request[F]): F[A] = for {
                metadataPartial <- metadataRequestDecoder.decodeRequest(request)
                bodyPartial <- request.as[PartialData[A]]
              } yield PartialData.unsafeReconcile(metadataPartial, bodyPartial)
            }
          case HttpRestSchema.Empty(value) =>
            new RequestDecoder[F, A] {
              def decodeRequest(request: Request[F]): F[A] = F.pure(value)
            }
            //format: on
        }
      }
    }

}
