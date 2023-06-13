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
import org.http4s.Response
import smithy4s.ConstraintError
import smithy4s.Errorable
import smithy4s.PartialData
import smithy4s.capability.Covariant
import smithy4s.http.HttpDiscriminator
import smithy4s.http.HttpErrorSelector
import smithy4s.http.HttpRestSchema
import smithy4s.http.Metadata
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema._

trait ResponseDecoder[F[_], A] {
  def decodeResponse(response: Response[F]): F[A]
}

object ResponseDecoder {

  /**
    * Creates a response decoder that dispatches the response to
    * the correct alternative, based on some discriminator.
    */
  def forError[F[_]: Concurrent, E](
      maybeErrorable: Option[Errorable[E]],
      decoderCompiler: CachedSchemaCompiler[ResponseDecoder[F, *]],
      discriminate: Response[F] => F[Option[HttpDiscriminator]]
  ): ResponseDecoder[F, E] =
    discriminating(
      discriminate,
      HttpErrorSelector(maybeErrorable, decoderCompiler)
    )

  /**
    * Creates a response decoder that dispatches the response to
    * the correct alternative, based on some discriminator, and
    * then upcasts the error as a throwable
    */
  def forErrorAsThrowable[F[_]: Concurrent, E](
      maybeErrorable: Option[Errorable[E]],
      decoderCompiler: CachedSchemaCompiler[ResponseDecoder[F, *]],
      discriminate: Response[F] => F[Option[HttpDiscriminator]]
  ): ResponseDecoder[F, Throwable] =
    discriminating(
      discriminate,
      HttpErrorSelector.asThrowable(maybeErrorable, decoderCompiler)
    )

  /**
    * Creates a response decoder that dispatches  the response
    * to a given decoder, based on some discriminator.
    */
  def discriminating[F[_]: Concurrent, Discriminator, E](
      discriminate: Response[F] => F[Option[Discriminator]],
      select: Discriminator => Option[ResponseDecoder[F, E]]
  ): ResponseDecoder[F, E] = {
    new ResponseDecoder[F, E] {
      def decodeResponse(response: Response[F]): F[E] =
        response.toStrict(None).flatMap { strictResponse =>
          discriminate(strictResponse).map(_.flatMap(select)).flatMap {
            case Some(decoder) => decoder.decodeResponse(strictResponse)
            case None =>
              val code = strictResponse.status.code
              val headers = getHeaders(strictResponse)
              strictResponse.as[String].flatMap { body =>
                MonadThrow[F].raiseError(
                  smithy4s.http.UnknownErrorResponse(code, headers, body)
                )
              }
          }
        }
    }
  }

  implicit def covariantResponseDecoder[F[_]: MonadThrow]
      : Covariant[ResponseDecoder[F, *]] =
    new Covariant[ResponseDecoder[F, *]] {
      def map[A, B](fa: ResponseDecoder[F, A])(
          f: A => B
      ): ResponseDecoder[F, B] = new ResponseDecoder[F, B] {
        def decodeResponse(response: Response[F]): F[B] =
          fa.decodeResponse(response).map(f)
      }

      def emap[A, B](fa: ResponseDecoder[F, A])(
          f: A => Either[ConstraintError, B]
      ): ResponseDecoder[F, B] = new ResponseDecoder[F, B] {
        def decodeResponse(response: Response[F]): F[B] =
          fa.decodeResponse(response).map(f).flatMap(_.liftTo[F])
      }
    }

  def fromEntityDecoder[F[_], A](implicit
      F: MonadThrow[F],
      entityDecoder: EntityDecoder[F, A]
  ): ResponseDecoder[F, A] = new ResponseDecoder[F, A] {
    def decodeResponse(response: Response[F]): F[A] = response.as[A]
  }

  /**
    * Creates a ResponseDecoder that decodes an HTTP message by looking at the
    * metadata.
    *
    * NB: This decoder assumes that incoming requests have been enriched with pre-extracted
    * path-parameters in the vault.
    */
  def fromMetadataDecoder[F[_]: Concurrent, A](
      metadataDecoder: Metadata.Decoder[A],
      drainMessage: Boolean
  ): ResponseDecoder[F, A] = new ResponseDecoder[F, A] {

    def decodeResponse(response: Response[F]): F[A] = {
      val metadata = getResponseMetadata(response)
      val decode = MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
      val drain = response.body.compile.drain.whenA(drainMessage)
      decode <* drain
    }
  }

  def rpcSchemaCompiler[F[_]](
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  )(implicit F: MonadThrow[F]): CachedSchemaCompiler[ResponseDecoder[F, *]] =
    new CachedSchemaCompiler[ResponseDecoder[F, *]] {
      type Cache = entityDecoderCompiler.Cache
      def createCache(): Cache =
        entityDecoderCompiler.createCache()

      def fromSchema[A](
          schema: Schema[A],
          cache: Cache
      ): ResponseDecoder[F, A] =
        fromEntityDecoder(F, entityDecoderCompiler.fromSchema(schema, cache))
      def fromSchema[A](schema: Schema[A]): ResponseDecoder[F, A] =
        fromEntityDecoder(F, entityDecoderCompiler.fromSchema(schema))
    }

  /**
    * A compiler for ResponseDecoder that abides by REST-semantics :
    * fields that are annotated with `httpLabel`, `httpHeader`, `httpQuery`,
    * `httpStatusCode` ... are decoded from the corresponding metadata.
    *
    * The rest is decoded from the body.
    */
  def restSchemaCompiler[F[_]](
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  )(implicit
      F: Concurrent[F]
  ): CachedSchemaCompiler[ResponseDecoder[F, *]] =
    new CachedSchemaCompiler[ResponseDecoder[F, *]] {
      type MetadataCache = Metadata.Decoder.Cache
      type EntityCache = entityDecoderCompiler.Cache
      type Cache = (EntityCache, MetadataCache)
      def createCache(): Cache = {
        val eCache = entityDecoderCompiler.createCache()
        val mCache = Metadata.Decoder.createCache()
        (eCache, mCache)
      }
      def fromSchema[A](schema: Schema[A]): ResponseDecoder[F, A] =
        fromSchema(schema, createCache())

      def fromSchema[A](
          fullSchema: Schema[A],
          cache: Cache
      ): ResponseDecoder[F, A] = {
        HttpRestSchema(fullSchema) match {
          case HttpRestSchema.OnlyMetadata(metadataSchema) =>
            // The data can be fully decoded from the metadata.
            val metadataDecoder =
              Metadata.Decoder.fromSchema(metadataSchema, cache._2)
            ResponseDecoder.fromMetadataDecoder(
              metadataDecoder,
              drainMessage = true
            )
          case HttpRestSchema.OnlyBody(bodySchema) =>
            // The data can be fully decoded from the body
            implicit val bodyDecoder: EntityDecoder[F, A] =
              entityDecoderCompiler.fromSchema(bodySchema, cache._1)
            ResponseDecoder.fromEntityDecoder(F, bodyDecoder)

          case HttpRestSchema.MetadataAndBody(metadataSchema, bodySchema) =>
            val metadataDecoder =
              Metadata.Decoder.fromSchema(metadataSchema, cache._2)
            val metadataResponseDecoder =
              ResponseDecoder.fromMetadataDecoder[F, PartialData[A]](
                metadataDecoder,
                drainMessage = false
              )
            implicit val bodyDecoder: EntityDecoder[F, PartialData[A]] =
              entityDecoderCompiler.fromSchema(bodySchema, cache._1)

            // format: off
            new ResponseDecoder[F, A] {

              def decodeResponse(response: Response[F]): F[A] = for {
                metadataPartial <- metadataResponseDecoder.decodeResponse(response)
                bodyPartial <- response.as[PartialData[A]]
              } yield PartialData.unsafeReconcile(metadataPartial, bodyPartial)
            }
          case HttpRestSchema.Empty(value) =>
            new ResponseDecoder[F, A] {
              def decodeResponse(response: Response[F]): F[A] = F.pure(value)
            }
            //format: on
        }
      }
    }

}
