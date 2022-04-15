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
import smithy.api.Pattern
import smithy.api.Length

/**
  * A type-refinement, associated to a runtime-representation of a constraint.
  */
trait Refinement[A, B] { self =>

  /**
    * The reified constraint associated to the refinement
    */
  type Constraint
  def tag: ShapeTag[Constraint]
  def constraint: Constraint
  def apply(a: A): Either[String, B]

  final val asFunction: A => Either[ConstraintError, B] =
    (a: A) =>
      apply(a).left.map(msg =>
        ConstraintError(Hints.Binding(tag, constraint), msg)
      )

  final val asThrowingFunction: A => B =
    apply(_) match {
      case Left(msg) =>
        throw ConstraintError(Hints.Binding(tag, constraint), msg)
      case Right(b) => b
    }

  final def contramap[A0](f: A0 => A): Refinement.Aux[Constraint, A0, B] =
    new Refinement[A0, B] {
      type Constraint = self.Constraint
      def tag: ShapeTag[Constraint] = self.tag
      def constraint: Constraint = self.constraint
      def apply(a0: A0): Either[String, B] = self(f(a0))
    }

  final def map[B0](f: B => B0): Refinement.Aux[Constraint, A, B0] =
    new Refinement[A, B0] {
      type Constraint = self.Constraint
      def tag: ShapeTag[Constraint] = self.tag
      def constraint: Constraint = self.constraint
      def apply(a: A): Either[String, B0] = self(a).map(f)
    }

}

object Refinement {

  trait Applicable[C, A, B] { self =>
    def tag: ShapeTag[C]
    def make(c: C): Refinement[A, B] { type Constraint = C }
    def contramap[A0](f: A0 => A): Applicable[C, A0, B] =
      new Applicable[C, A0, B] {
        def tag = self.tag
        def make(c: C): Refinement[A0, B] { type Constraint = C } =
          self.make(c).contramap(f)
      }

    def map[B0](f: B => B0): Applicable[C, A, B0] =
      new Applicable[C, A, B0] {
        def tag = self.tag
        def make(c: C): Refinement[A, B0] { type Constraint = C } =
          self.make(c).map(f)
      }
  }

  type Checked[C, A] = Applicable[C, A, A]

  abstract class CheckedImpl[C, A](implicit _tag: ShapeTag[C])
      extends Refinement.Applicable[C, A, A] {

    val tag: ShapeTag[C] = _tag

    def check(c: C): A => Either[String, Unit]
    final def make(c: C): Aux[C, A, A] = new Refinement[A, A] {
      type Constraint = C
      final val tag: ShapeTag[C] = _tag
      final val constraint: C = c
      final val run = check(c)
      final def apply(a: A): Either[String, A] = run(a).map(_ => a)
    }

  }

  type Aux[C, A, B] = Refinement[A, B] { type Constraint = C }

  object Applicable {

    implicit def isomorphismConstraint[C, A, A0](implicit
        constraintOnA: Checked[C, A],
        iso: Isomorphism[A, A0]
    ): Checked[C, A0] = constraintOnA.contramap(iso.from).map(iso.to)

    implicit val stringLengthConstraint: Checked[Length, String] =
      new LengthConstraint[String](_.length)

    implicit def iterableLengthConstraint[C[_], A](implicit
        ev: C[A] <:< Iterable[A]
    ): Checked[Length, C[A]] =
      new LengthConstraint[C[A]](ca => ev(ca).size)

    implicit def mapLengthConstraint[K, V]: Checked[Length, Map[K, V]] =
      new LengthConstraint[Map[K, V]](_.size)

    class LengthConstraint[A](getLength: A => Int)
        extends CheckedImpl[Length, A] {

      def check(
          lengthHint: Length
      ): A => Either[String, Unit] = { (a: A) =>
        val length = getLength(a)
        (lengthHint.min, lengthHint.max) match {
          case (Some(min), Some(max)) =>
            if (length >= min && length <= max) Right(())
            else
              Left(
                s"length required to be >= $min and <= $max, but was $length"
              )
          case (Some(min), None) =>
            if (length >= min) Right(())
            else
              Left(
                s"length required to be >= $min, but was $length"
              )
          case (None, Some(max)) =>
            if (length <= max) Right(())
            else
              Left(
                s"length required to be <= $max, but was $length"
              )
          case (None, None) => Right(())
        }
      }

    }

    implicit val stringPatternConstraints: Checked[Pattern, String] =
      new CheckedImpl[Pattern, String] {

        def check(
            pattern: Pattern
        ): String => Either[String, Unit] = {
          val regex = pattern.value.r
          (input: String) =>
            if (regex.findFirstIn(input).isDefined) Right(())
            else
              Left(
                s"String '$input' does not match pattern '${pattern.value}'"
              )
        }

      }

    implicit def numericRangeConstraints[N: Numeric]
        : Checked[smithy.api.Range, N] = new CheckedImpl[smithy.api.Range, N] {

      def check(
          range: smithy.api.Range
      ): N => Either[String, Unit] = {
        val N = implicitly[Numeric[N]]

        (n: N) =>
          val value = BigDecimal(N.toDouble(n))
          (range.min, range.max) match {
            case (Some(min), Some(max)) =>
              if (value >= min && value <= max) Right(())
              else
                Left(
                  s"Input must be >= $min and <= $max, but was $value"
                )
            case (None, Some(max)) =>
              if (value <= max) Right(())
              else
                Left(
                  s"Input must be <= $max, but was $value"
                )
            case (Some(min), None) =>
              if (value >= min) Right(())
              else
                Left(
                  s"Input must be >= $min, but was $value"
                )
            case (None, None) => Right(())
          }
      }
    }
  }

}
