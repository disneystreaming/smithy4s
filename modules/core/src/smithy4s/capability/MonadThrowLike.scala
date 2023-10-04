/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.capability

import smithy4s.kinds.PolyFunction

trait MonadThrowLike[F[_]] extends Zipper[F] {
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def raiseError[A](e: Throwable): F[A]
  def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A]

  final def liftEither[E <: Throwable, A](either: Either[E, A]): F[A] =
    either match {
      case Left(error)  => raiseError(error)
      case Right(value) => pure(value)
    }

}

object MonadThrowLike {

  def apply[F[_]](implicit ev: MonadThrowLike[F]): ev.type = ev

  def liftEitherK[F[_]: MonadThrowLike, E <: Throwable]
      : PolyFunction[Either[E, *], F] = new PolyFunction[Either[E, *], F] {
    def apply[A0](either: Either[E, A0]): F[A0] =
      MonadThrowLike[F].liftEither(either)
  }

  def mapErrorK[F[_]](
      pf: PartialFunction[Throwable, Throwable]
  )(implicit F: MonadThrowLike[F]): PolyFunction[F, F] =
    new PolyFunction[F, F] {
      def apply[A](fa: F[A]): F[A] = F.handleErrorWith(fa) { throwable =>
        if (pf.isDefinedAt(throwable)) F.raiseError(pf(throwable))
        else F.raiseError(throwable)
      }
    }

}
