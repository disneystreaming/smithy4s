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

import smithy.api.Length
import smithy.api.Pattern

trait Constraints { self =>

  import Constraints._

  def checkString(hints: Hints): Option[String => Either[ConstraintError, Unit]]

  def checkCollection[A](
      hints: Hints
  ): Option[Iterable[A] => Either[ConstraintError, Unit]]

  def checkNumeric[N: Numeric](
      hints: Hints
  ): Option[N => Either[ConstraintError, Unit]]

  def ++(other: Constraints): Constraints = new Constraints {

    private def combine[A](
        method: Constraints => Option[A => Either[ConstraintError, Unit]]
    ): Option[A => Either[ConstraintError, Unit]] = {
      (method(self), method(other)) match {
        case (Some(left), Some(right)) =>
          Some((a: A) => left(a).flatMap(_ => right(a)))
        case (None, None)   => None
        case (mLeft, None)  => mLeft
        case (None, mRight) => mRight
      }

    }

    def checkString(
        hints: Hints
    ): Option[String => Either[ConstraintError, Unit]] = {
      combine(_.checkString(hints))
    }
    def checkCollection[A](
        hints: Hints
    ): Option[Iterable[A] => Either[ConstraintError, Unit]] = {
      combine(_.checkCollection(hints))
    }
    def checkNumeric[N: Numeric](
        hints: Hints
    ): Option[N => Either[ConstraintError, Unit]] = {
      combine(_.checkNumeric(hints))
    }
  }
}

object Constraints {

  case object NoopConstraints extends StubConstraints {}

  // format: off
  trait StubConstraints extends Constraints {
    def checkString(hints: Hints): Option[String => Either[ConstraintError, Unit]] = None
    def checkCollection[A](hints: Hints): Option[Iterable[A] => Either[ConstraintError, Unit]] = None
    def checkNumeric[N: Numeric](hints: Hints): Option[N => Either[ConstraintError, Unit]] = None
  }
  // format: on

  object LengthConstraints extends StubConstraints {

    private def checkLength[A](
        hints: Hints,
        getLength: A => Int
    ): Option[A => Either[ConstraintError, Unit]] = {
      hints
        .get(Length)
        .map { lengthHint => (a: A) =>
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

    override def checkString(
        hints: Hints
    ): Option[String => Either[ConstraintError, Unit]] =
      checkLength[String](hints, _.size)
    override def checkCollection[A](
        hints: Hints
    ): Option[Iterable[A] => Either[ConstraintError, Unit]] =
      checkLength[Iterable[A]](hints, _.size)

  }

  object PatternConstraints extends StubConstraints {

    private def checkPattern(
        hints: Hints
    ): Option[String => Either[ConstraintError, Unit]] =
      hints
        .get(Pattern)
        .map { pattern =>
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

    override def checkString(
        hints: Hints
    ): Option[String => Either[ConstraintError, Unit]] =
      checkPattern(hints)
  }

  object RangeConstraints extends StubConstraints {

    override def checkNumeric[N: Numeric](
        hints: Hints
    ): Option[N => Either[ConstraintError, Unit]] = {
      val N = implicitly[Numeric[N]]
      val maybeRange = hints.get(smithy.api.Range)

      maybeRange
        .map { range => (n: N) =>
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
  }

  object UniqueItemsConstraints extends StubConstraints {

    override def checkCollection[A](
        hints: Hints
    ): Option[Iterable[A] => Either[ConstraintError, Unit]] = {
      val maybeUniqueHint = hints.get(smithy.api.UniqueItems)
      maybeUniqueHint
        .map { hint => (itr: Iterable[A]) =>
          if (itr.toSet.size != itr.size) {
            Left(
              ConstraintError(
                hint,
                "List contains duplicate items while marked with UniqueItems trait"
              )
            )
          } else {
            Right(())
          }
        }
    }
  }

  val defaultConstraints: Constraints = LengthConstraints ++
    RangeConstraints ++
    PatternConstraints ++
    UniqueItemsConstraints

  final case class ConstraintError(hint: Hint, message: String)
      extends Throwable
      with scala.util.control.NoStackTrace

}
