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
import smithy4s.Errorable
import smithy4s.http.HttpDiscriminator
import smithy4s.http.HttpErrorSelector
import smithy4s.http.HttpRestSchema
import smithy4s.http.Metadata
import smithy4s.kinds.PolyFunction
import smithy4s.schema.CachedSchemaCompiler

object ResponseDecoder {

  type CachedCompiler[F[_]] = CachedSchemaCompiler[ResponseDecoder[F, *]]

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
      def read(response: Response[F]): F[E] =
        response.toStrict(None).flatMap { strictResponse =>
          discriminate(strictResponse).map(_.flatMap(select)).flatMap {
            case Some(decoder) => decoder.read(strictResponse)
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

  def fromEntityDecoder[F[_], A](implicit
      F: MonadThrow[F],
      entityDecoder: EntityDecoder[F, A]
  ): ResponseDecoder[F, A] = new ResponseDecoder[F, A] {
    def read(response: Response[F]): F[A] = response.as[A]
  }

  /**
    * Creates a ResponseDecoder that decodes an HTTP message by looking at the
    * metadata.
    *
    * NB: This decoder assumes that incoming requests have been enriched with pre-extracted
    * path-parameters in the vault.
    */
  def fromMetadataDecoder[F[_]: MonadThrow, A](
      metadataDecoder: Metadata.Decoder[A]
  ): ResponseDecoder[F, A] = new ResponseDecoder[F, A] {

    def read(response: Response[F]): F[A] = {
      val metadata = getResponseMetadata(response)
      MonadThrow[F].fromEither(metadataDecoder.decode(metadata))
    }
  }

  def fromMetadataDecoderK[F[_]: MonadThrow]
      : PolyFunction[Metadata.Decoder, ResponseDecoder[F, *]] =
    new PolyFunction[Metadata.Decoder, ResponseDecoder[F, *]] {
      def apply[A](fa: Metadata.Decoder[A]): ResponseDecoder[F, A] =
        fromMetadataDecoder(fa)
    }

  def rpcSchemaCompiler[F[_]: Concurrent](
      entityDecoderCompiler: CachedSchemaCompiler[EntityDecoder[F, *]]
  ): CachedSchemaCompiler[ResponseDecoder[F, *]] =
    MediaDecoder.rpcSchemaCompiler(entityDecoderCompiler)

  /**
    * A compiler for ResponseDecoder that abides by REST-semantics :
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
  ): CachedSchemaCompiler[ResponseDecoder[F, *]] = {
    val metadataCompiler = metadataDecoderCompiler.mapK(fromMetadataDecoderK[F])
    val bodyCompiler =
      entityDecoderCompiler.mapK(MediaDecoder.fromEntityDecoderK)
    HttpRestSchema.combineReaderCompilers[F, Response[F]](
      metadataCompiler,
      bodyCompiler
    )
  }

}
