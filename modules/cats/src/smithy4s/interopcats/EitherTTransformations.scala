/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.interopcats

import cats.MonadThrow
import cats.data.EitherT
import cats.syntax.all._
import smithy4s.Transformation

object EitherTTransformations {

  def absorbError[F[_]: MonadThrow]
      : Transformation.AbsorbError[EitherT[F, *, *], F] =
    new Transformation.AbsorbError[EitherT[F, *, *], F] {
      def apply[E, A](
          fa: EitherT[F, E, A],
          injectError: E => Throwable
      ): F[A] =
        fa.leftMap(injectError).rethrowT
    }

  def surfaceError[F[_]: MonadThrow]
      : Transformation.SurfaceError[F, EitherT[F, *, *]] =
    new Transformation.SurfaceError[F, EitherT[F, *, *]] {
      def apply[E, A](
          fa: F[A],
          projectError: Throwable => Option[E]
      ): EitherT[F, E, A] =
        EitherT {
          fa.map(Right(_): Either[E, A]).recoverWith { case t =>
            projectError(t) match {
              case Some(e) => MonadThrow[F].pure(Left(e))
              case None    => MonadThrow[F].raiseError(t)
            }
          }
        }
    }

}
