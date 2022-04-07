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

import capability.Isomorphism
import smithy.api.Length
import smithy.api.Pattern

trait Constraint[C, A] { self =>

  def tag: ShapeTag[C]

  def check(constraint: C): A => Either[ConstraintError, Unit]

}

object Constraint {

  def apply[C, A](implicit constraint: Constraint[C, A]): Constraint[C, A] =
    constraint

  implicit def isomorphismConstraint[C, A, B](implicit
      constraintOnA: Constraint[C, A],
      iso: Isomorphism[A, B]
  ): Constraint[C, B] = new Constraint[C, B] {

    def tag = constraintOnA.tag

    def check(constraint: C): B => Either[ConstraintError, Unit] = {
      val fun = constraintOnA.check(constraint)
      fun.compose(iso.from)
    }
  }

  implicit val stringLengthConstraint: Constraint[Length, String] =
    new LengthConstraint[String](_.length)

  implicit def iterableLengthConstraint[C[_], A](implicit
      ev: C[A] <:< Iterable[A]
  ): Constraint[Length, C[A]] =
    new LengthConstraint[C[A]](ca => ev(ca).size)

  implicit def mapLengthConstraint[K, V]: Constraint[Length, Map[K, V]] =
    new LengthConstraint[Map[K, V]](_.size)

  class LengthConstraint[A](getLength: A => Int) extends Constraint[Length, A] {

    val tag = Length

    def check(
        lengthHint: Length
    ): A => Either[ConstraintError, Unit] = { (a: A) =>
      val length = getLength(a)
      (lengthHint.min, lengthHint.max) match {
        case (Some(min), Some(max)) =>
          if (length >= min && length <= max) Right(())
          else
            Left(
              ConstraintError(
                lengthHint,
                s"length required to be >= $min and <= $max, but was $length"
              )
            )
        case (Some(min), None) =>
          if (length >= min) Right(())
          else
            Left(
              ConstraintError(
                lengthHint,
                s"length required to be >= $min, but was $length"
              )
            )
        case (None, Some(max)) =>
          if (length <= max) Right(())
          else
            Left(
              ConstraintError(
                lengthHint,
                s"length required to be <= $max, but was $length"
              )
            )
        case (None, None) => Right(())
      }
    }

  }

  implicit val stringPatternConstraints: Constraint[Pattern, String] =
    new Constraint[Pattern, String] {

      val tag: ShapeTag[Pattern] = ShapeTag[Pattern]

      def check(
          pattern: Pattern
      ): String => Either[ConstraintError, Unit] = {
        val regex = pattern.value.r
        (input: String) =>
          if (regex.findFirstIn(input).isDefined) Right(())
          else
            Left(
              ConstraintError(
                pattern,
                s"String '$input' does not match pattern '${pattern.value}'"
              )
            )
      }

    }

  implicit def numericRangeConstraints[N: Numeric]
      : Constraint[smithy.api.Range, N] = new Constraint[smithy.api.Range, N] {

    val tag: ShapeTag[smithy.api.Range] = ShapeTag[smithy.api.Range]

    def check(
        range: smithy.api.Range
    ): N => Either[ConstraintError, Unit] = {
      val N = implicitly[Numeric[N]]

      (n: N) =>
        val value = BigDecimal(N.toDouble(n))
        (range.min, range.max) match {
          case (Some(min), Some(max)) =>
            if (value >= min && value <= max) Right(())
            else
              Left(
                ConstraintError(
                  range,
                  s"Input must be >= $min and <= $max, but was $value"
                )
              )
          case (None, Some(max)) =>
            if (value <= max) Right(())
            else
              Left(
                ConstraintError(
                  range,
                  s"Input must be <= $max, but was $value"
                )
              )
          case (Some(min), None) =>
            if (value >= min) Right(())
            else
              Left(
                ConstraintError(
                  range,
                  s"Input must be >= $min, but was $value"
                )
              )
          case (None, None) => Right(())
        }
    }
  }

  implicit def collectionsUniqueItemsConstraint[C[_], A](implicit
      ev: C[A] <:< Iterable[A]
  ): Constraint[smithy.api.UniqueItems, C[A]] =
    new Constraint[smithy.api.UniqueItems, C[A]] {

      val tag: ShapeTag[smithy.api.UniqueItems] =
        ShapeTag[smithy.api.UniqueItems]

      def check(
          unique: smithy.api.UniqueItems
      ): C[A] => Either[ConstraintError, Unit] = { (ca: C[A]) =>
        val itr = ev(ca)
        if (itr.toSet.size != itr.size) {
          Left(
            ConstraintError(
              unique,
              "List contains duplicate items while marked with UniqueItems trait"
            )
          )
        } else {
          Right(())
        }
      }
    }

}
