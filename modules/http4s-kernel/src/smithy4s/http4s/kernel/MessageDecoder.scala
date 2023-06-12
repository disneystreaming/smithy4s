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

import cats.~>
import org.http4s.Request
import org.http4s.Response
import cats.MonadThrow
import org.http4s.EntityDecoder
import smithy4s.schema._
import smithy4s.PartialData
import cats.effect.Concurrent
import smithy4s.http.HttpRestSchema
import smithy4s.http.Metadata
import cats.syntax.all._

trait MessageDecoder[F[_], A]
    extends RequestDecoder[F, A]
    with ResponseDecoder[F, A]

object MessageDecoder {
  type Middleware[F[_], A] = MessageDecoder[F, A] => MessageDecoder[F, A]
  type MiddlewareK[F[_]] = MessageDecoder[F, *] ~> MessageDecoder[F, *]

  def noop[F[_]]: MessageDecoder.MiddlewareK[F] =
    new (MessageDecoder[F, *] ~> MessageDecoder[F, *]) {
      def apply[A](fa: MessageDecoder[F, A]): MessageDecoder[F, A] = fa
    }

  def fromRequestDecoder[F[_]](
      rdm: RequestDecoder.MiddlewareK[F]
  ): MessageDecoder.MiddlewareK[F] =
    new (MessageDecoder[F, *] ~> MessageDecoder[F, *]) {
      def apply[A](fa: MessageDecoder[F, A]): MessageDecoder[F, A] =
        new MessageDecoder[F, A] {
          def decodeRequest(request: Request[F]): F[A] =
            rdm(fa).decodeRequest(request)
          def decodeResponse(response: Response[F]): F[A] =
            fa.decodeResponse(response)
        }
    }

  def fromEntityDecoder[F[_], A](implicit
      F: MonadThrow[F],
      entityDecoder: EntityDecoder[F, A]
  ): MessageDecoder[F, A] = new MessageDecoder[F, A] {
    def decodeRequest(request: Request[F]): F[A] = request.as[A]

    def decodeResponse(response: Response[F]): F[A] = response.as[A]
  }

  /**
    * Creates a MessageDecoder that decodes an HTTP message by looking at the
    * metadata.
    *
    * NB: This decoder assumes that incoming requests have been enriched with pre-extracted
    * path-parameters in the vault.
    */
  def fromMetadataDecoder[F[_]: Concurrent, A](
      metadataDecoder: Metadata.Decoder[A],
      drainMessage: Boolean
  ): MessageDecoder[F, A] = new MessageDecoder[F, A] {
    def decodeRequest(request: Request[F]): F[A] = {
      // TODO better recovery when the pathParams cannot be retrieved from the vault
      val queryParams =
        request.attributes.lookup(pathParamsKey).getOrElse(Map.empty)
      val metadata = getRequestMetadata(queryParams, request)
      val drain = request.body.compile.drain.whenA(drainMessage)
      val decode = MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
      decode <* drain
    }

    def decodeResponse(response: Response[F]): F[A] = {
      val metadata = getResponseMetadata(response)
      val decode = MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
      val drain = response.body.compile.drain.whenA(drainMessage)
      decode <* drain
    }
  }

  def rpcSchemaCompiler[F[_]](
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  )(implicit F: MonadThrow[F]): CachedSchemaCompiler[MessageDecoder[F, *]] =
    new CachedSchemaCompiler[MessageDecoder[F, *]] {
      type Cache = entityDecoderCompiler.Cache
      def createCache(): Cache =
        entityDecoderCompiler.createCache()

      def fromSchema[A](schema: Schema[A], cache: Cache): MessageDecoder[F, A] =
        fromEntityDecoder(F, entityDecoderCompiler.fromSchema(schema, cache))
      def fromSchema[A](schema: Schema[A]): MessageDecoder[F, A] =
        fromEntityDecoder(F, entityDecoderCompiler.fromSchema(schema))
    }

  /**
    * A compiler for MessageDecoder that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`,
    * `httpStatusCode` ... are decoded from the corresponding metadata.
    *
    * The rest is decoded from the body.
    */
  def restSchemaCompiler[F[_]](
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  )(implicit
      F: Concurrent[F]
  ): CachedSchemaCompiler[MessageDecoder[F, *]] =
    new CachedSchemaCompiler[MessageDecoder[F, *]] {
      type MetadataCache = Metadata.Decoder.Cache
      type EntityCache = entityDecoderCompiler.Cache
      type Cache = (EntityCache, MetadataCache)
      def createCache(): Cache = {
        val eCache = entityDecoderCompiler.createCache()
        val mCache = Metadata.Decoder.createCache()
        (eCache, mCache)
      }
      def fromSchema[A](schema: Schema[A]): MessageDecoder[F, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          fullSchema: Schema[A],
          cache: Cache
      ): MessageDecoder[F, A] = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata.
            val metadataDecoder =
              Metadata.Decoder.fromSchema(metadataSchema, cache._2)
            MessageDecoder.fromMetadataDecoder(
              metadataDecoder,
              drainMessage = true
            )
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            implicit val bodyDecoder: EntityDecoder[F, A] =
              entityDecoderCompiler.fromSchema(bodySchema, cache._1)
            MessageDecoder.fromEntityDecoder(F, bodyDecoder)

          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataDecoder =
              Metadata.Decoder.fromSchema(metadataSchema, cache._2)
            val metadataMessageDecoder =
              MessageDecoder.fromMetadataDecoder[F, PartialData[A]](
                metadataDecoder,
                drainMessage = false
              )
            implicit val bodyDecoder: EntityDecoder[F, PartialData[A]] =
              entityDecoderCompiler.fromSchema(bodySchema, cache._1)

            // format: off
            new MessageDecoder[F, A] {
              def decodeRequest(request: Request[F]): F[A] = for {
                metadataPartial <- metadataMessageDecoder.decodeRequest(request)
                bodyPartial <- request.as[PartialData[A]]
              } yield PartialData.unsafeReconcile(metadataPartial, bodyPartial)

              def decodeResponse(response: Response[F]): F[A] = for {
                metadataPartial <- metadataMessageDecoder.decodeResponse(response)
                bodyPartial <- response.as[PartialData[A]]
              } yield PartialData.unsafeReconcile(metadataPartial, bodyPartial)
            }
          case HttpRestSchema.Empty(value) =>
            new MessageDecoder[F, A] {
              def decodeRequest(request: Request[F]): F[A] = F.pure(value)
              def decodeResponse(response: Response[F]): F[A] = F.pure(value)
            }
            //format: on
        }
      }
    }

}
