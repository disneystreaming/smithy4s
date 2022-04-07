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

import cats.Show
import cats.effect.IO
import cats.syntax.all._
import org.scalacheck.Gen
import smithy.api.Length
import smithy.api.Pattern
import smithy.api.UniqueItems
import weaver._
import weaver.scalacheck.Checkers

object ConstraintsSpec extends SimpleIOSuite with Checkers {

  pureTest("LengthConstraints checks strings for length - valid input") {
    val hint = Length(Some(10), Some(20))
    val result = Constraint[Length, String].check(hint)("#" * 10)
    expect(result == Right(()))
  }

  pureTest("LengthConstraints checks strings for length - invalid input") {
    val hint = Length(Some(10), Some(20))
    val result = Constraint[Length, String].check(hint)("#" * 21)
    expect(
      result == Left(
        ConstraintError(
          hint,
          "length required to be >= 10 and <= 20, but was 21"
        )
      )
    )
  }

  test("LengthConstraints String properties") {
    runLengthTest[String](
      _.size,
      Gen.alphaNumStr
    )
  }

  test("LengthConstraints collection properties - List") {
    runLengthTest[List[Byte]](
      _.size,
      Gen.alphaNumStr.map(_.getBytes.toList)
    )
  }

  pureTest("PatternConstraints checks strings for pattern - valid input") {
    val hint = Pattern("\\w+")
    val result =
      Constraint[Pattern, String].check(hint)("!hello!")
    expect(result == Right(()))
  }

  pureTest("PatternConstraints checks strings for pattern - invalid input") {
    val hint = Pattern("\\w+")
    val result = Constraint[Pattern, String].check(hint)("!!")
    expect(
      result == Left(
        ConstraintError(
          hint,
          "String '!!' does not match pattern '\\w+'"
        )
      )
    )
  }

  test("RangeConstraints Numeric check input is in range - long") {
    val gen = for {
      min <- Gen.option(Gen.posNum[BigDecimal])
      max <- Gen.option(Gen.posNum[BigDecimal])
      input <- Gen.long
    } yield (min, max, input)
    forall(gen) { case (min, max, input) =>
      val hint = smithy.api.Range(min, max)
      val f = Constraint[smithy.api.Range, Long].check(hint)
      val result = f(input)
      result match {
        case Left(_) => expect(min.exists(_ > input) || max.exists(_ < input))
        case Right(_) =>
          expect(min.forall(_ <= input) && max.forall(_ >= input))
      }
    }
  }

  test("UniqueItemsConstraints checks for duplicates in Lists - valid inputs") {
    forall(Gen.listOf(Gen.long).map(_.distinct)) { input =>
      val hint = UniqueItems()
      val result =
        Constraint[UniqueItems, List[Long]].check(hint)(input)
      expect(result == Right(()))
    }
  }

  pureTest(
    "UniqueItemsConstraints checks for duplicates in Lists - invalid input"
  ) {
    val hint = UniqueItems()
    val result =
      Constraint[UniqueItems, List[Int]].check(hint)(List(1, 2, 3, 1))
    expect(
      result == Left(
        ConstraintError(
          hint,
          "List contains duplicate items while marked with UniqueItems trait"
        )
      )
    )
  }

  private def runLengthTest[A: Show](
      getLength: A => Int,
      genA: Gen[A]
  )(implicit constraint: Constraint[Length, A]): IO[Expectations] = {
    val gen = for {
      min <- Gen.option(Gen.posNum[Long])
      max <- Gen.option(Gen.posNum[Long])
      input <- genA
    } yield (min, max, input)
    forall(gen) { case (min, max, input) =>
      val testLength = getLength(input)
      val hint = Length(min, max)
      val result = constraint.check(hint)(input)
      result match {
        case Left(_) =>
          expect(min.exists(_ > testLength) || max.exists(_ < testLength))
        case Right(_) =>
          expect(min.forall(_ <= testLength) && max.forall(_ >= testLength))
      }
    }
  }

}
