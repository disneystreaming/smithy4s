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
import org.http4s.Response
import smithy4s.ConstraintError
import smithy4s.Errorable
import smithy4s.capability.Covariant
import smithy4s.http.HttpDiscriminator
import smithy4s.http.HttpErrorSelector
import smithy4s.schema.CachedSchemaCompiler

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
}
