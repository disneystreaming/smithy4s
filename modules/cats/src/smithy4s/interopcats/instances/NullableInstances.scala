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

package smithy4s.interopcats.instances

import cats._
import cats.syntax.all._
import smithy4s.Nullable
import smithy4s.Nullable._
import scala.annotation.tailrec

trait NullableInstances {
  implicit def monoid[A](implicit sg: Semigroup[A]): Monoid[Nullable[A]] =
    Monoid.instance(
      Null,
      {
        case (Value(a), Value(b)) => Value(sg.combine(a, b))
        case (Value(a), Null)     => Value(a)
        case (Null, other)        => other
      }
    )

  implicit def hash[A](implicit in: Hash[A]): Hash[Nullable[A]] =
    Hash.by(_.toOption)

  implicit def show[A](implicit in: Show[A]): Show[Nullable[A]] = Show.show {
    case Value(a) => show"Value($a)"
    case Null     => "Null"
  }

  implicit val standardInstances: Monad[Nullable] with Traverse[Nullable] =
    new Monad[Nullable] with Traverse[Nullable] {

      override def traverse[G[_]: Applicative, A, B](
          fa: Nullable[A]
      )(f: A => G[B]): G[Nullable[B]] = fa match {
        case Null     => Applicative[G].pure(Null)
        case Value(a) => f(a).map(Value(_))
      }

      override def foldLeft[A, B](fa: Nullable[A], b: B)(f: (B, A) => B): B =
        fa.fold(b)(f(b, _))

      override def foldRight[A, B](fa: Nullable[A], lb: Eval[B])(
          f: (A, Eval[B]) => Eval[B]
      ): Eval[B] =
        fa.fold(lb)(f(_, lb))

      override def pure[A](x: A): Nullable[A] = Value(x)

      override def flatMap[A, B](
          fa: Nullable[A]
      )(f: A => Nullable[B]): Nullable[B] = fa.map(f) match {
        case Value(x) => x
        case Null     => Null
      }

      @tailrec
      override def tailRecM[A, B](
          a: A
      )(f: A => Nullable[Either[A, B]]): Nullable[B] = f(a) match {
        case Nullable.Null   => Null
        case Value(Right(b)) => Value(b)
        case Value(Left(a))  => tailRecM(a)(f)
      }
    }
}
