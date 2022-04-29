/*
 *  Copyright 2021 Disney Streaming
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

package smithy4s

import cats.Functor
import cats.syntax.all._
import cats.~>

package recursion {
  case class Fix[F[_]](unfix: F[Fix[F]])

  object t {
    val x = Fix[Option](Option(Fix[Option](None)))
  }
}

package object recursion {

  def hylo[F[_]: Functor, A, B](unfold: A => F[A], fold: F[B] => B)(a: A): B =
    fold(unfold(a).map(hylo(unfold, fold)))

  def cata[F[_]: Functor, B](fold: F[B] => B)(tree: Fix[F]): B =
    hylo[F, Fix[F], B](_.unfix, fold)(tree)

  def ana[F[_]: Functor, A](unfold: A => F[A])(a: A): Fix[F] =
    hylo[F, A, Fix[F]](unfold, Fix(_))(a)

  def preprocess[F[_]: Functor](nt: F ~> F)(tree: Fix[F]): Fix[F] =
    cata[F, Fix[F]](ff => Fix(nt(ff)))(tree)

}
