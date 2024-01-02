/*
 *  Copyright 2021-2024 Disney Streaming
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

package smithy4s.codegen
package internals

import cats.Functor
import cats.Monad
import cats.Traverse
import cats.syntax.all._
import cats.~>

private[internals] case class Fix[F[_]](unfix: F[Fix[F]])

/**
  * This package contains recursion-scheme implementations.
  * Because of the concision and the fact that we need a small subset of them,
  * we re-implement them as opposed to depending on a library.
  *
  * See libraries that offer larger sets of recursion-schemes (and docs) :
  * * Droste https://index.scala-lang.org/higherkindness/droste
  * * Matryoshka https://index.scala-lang.org/precog/matryoshka
  *
  */
private[internals] object recursion {

  def hylo[F[_]: Functor, A, B](unfold: A => F[A], fold: F[B] => B)(a: A): B =
    fold(unfold(a).map(hylo(unfold, fold)))

  def hyloM[M[_]: Monad, F[_]: Traverse, A, B](
      unfold: A => M[F[A]],
      fold: F[B] => M[B]
  )(a: A): M[B] = {
    type MF[T] = M[F[T]] // composition of M and F
    implicit val MF: Functor[MF] = Functor[M].compose(Functor[F])
    val F = Traverse[F]
    def foldM(mfmb: M[F[M[B]]]): M[B] = for {
      fmb <- mfmb
      fb <- F.sequence[M, B](fmb)
      f <- fold(fb)
    } yield f

    hylo[MF, A, M[B]](unfold, foldM)(a)
  }

  def cata[F[_]: Functor, B](fold: F[B] => B)(tree: Fix[F]): B =
    hylo[F, Fix[F], B](_.unfix, fold)(tree)

  def ana[F[_]: Functor, A](unfold: A => F[A])(a: A): Fix[F] =
    hylo[F, A, Fix[F]](unfold, Fix(_))(a)

  def anaM[M[_]: Monad, F[_]: Traverse, A](unfold: A => M[F[A]])(
      a: A
  ): M[Fix[F]] =
    hyloM[M, F, A, Fix[F]](unfold, x => Monad[M].pure(Fix(x)))(a)

  def preprocess[F[_]: Functor](nt: F ~> F)(tree: Fix[F]): Fix[F] =
    cata[F, Fix[F]](ff => Fix(nt(ff)))(tree)

}
