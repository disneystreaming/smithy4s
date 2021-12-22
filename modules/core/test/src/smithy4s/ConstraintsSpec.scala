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
    val hints = Hints(Length(Some(10), Some(20)))
    val result = Constraints.LengthConstraints.checkString(hints)("#" * 10)
    expect(result == Right(()))
  }

  pureTest("LengthConstraints checks strings for length - invalid input") {
    val hint = Length(Some(10), Some(20))
    val result =
      Constraints.LengthConstraints.checkString(Hints(hint))("#" * 21)
    expect(
      result == Left(
        Constraints.ConstraintError(
          hint,
          "length required to be >= 10 and <= 20, but was 21"
        )
      )
    )
  }

  test("LengthConstraints String properties") {
    runLengthTest[String](
      _.size,
      Gen.alphaNumStr,
      Constraints.LengthConstraints.checkString
    )
  }

  test("LengthConstraints collection properties - List") {
    runLengthTest[List[Byte]](
      _.size,
      Gen.alphaNumStr.map(_.getBytes.toList),
      Constraints.LengthConstraints.checkCollection
    )
  }

  test("LengthConstraints Number properties - long") {
    val gen = for {
      min <- Gen.option(Gen.posNum[Long])
      max <- Gen.option(Gen.posNum[Long])
      input <- Gen.long
    } yield (min, max, input)
    forall(gen) { case (min, max, input) =>
      val hint = Length(min, max)
      val f = Constraints.LengthConstraints.checkNumeric[Long](Hints(hint))
      expect(f(input) == Right(()))
    }
  }

  pureTest("PatternConstraints checks strings for pattern - valid input") {
    val hint = Pattern("\\w+")
    val result =
      Constraints.PatternConstraints.checkString(Hints(hint))("!hello!")
    expect(result == Right(()))
  }

  pureTest("PatternConstraints checks strings for pattern - invalid input") {
    val hint = Pattern("\\w+")
    val result = Constraints.PatternConstraints.checkString(Hints(hint))("!!")
    expect(
      result == Left(
        Constraints.ConstraintError(
          hint,
          "String '!!' does not match pattern '\\w+'"
        )
      )
    )
  }

  test("PatternConstraints collection properties - List") {
    val gen = for {
      pattern <- Gen.alphaNumStr
      input <- Gen.listOf(Gen.long)
    } yield (pattern, input)
    forall(gen) { case (pattern, input) =>
      val hint = Pattern(pattern)
      val result =
        Constraints.PatternConstraints.checkCollection(Hints(hint))(input)
      expect(result == Right(()))
    }
  }

  test("PatternConstraints Numeric properties - long") {
    val gen = for {
      pattern <- Gen.alphaNumStr
      input <- Gen.long
    } yield (pattern, input)
    forall(gen) { case (pattern, input) =>
      val hint = Pattern(pattern)
      val f = Constraints.PatternConstraints.checkNumeric[Long](Hints(hint))
      expect(f(input) == Right(()))
    }
  }

  test("RangeConstraints Numeric check input is in range - long") {
    val gen = for {
      min <- Gen.option(Gen.posNum[BigDecimal])
      max <- Gen.option(Gen.posNum[BigDecimal])
      input <- Gen.long
    } yield (min, max, input)
    forall(gen) { case (min, max, input) =>
      val hint = smithy.api.Range(min, max)
      val f = Constraints.RangeConstraints.checkNumeric[Long](Hints(hint))
      val result = f(input)
      result match {
        case Left(_) => expect(min.exists(_ > input) || max.exists(_ < input))
        case Right(_) =>
          expect(min.forall(_ <= input) && max.forall(_ >= input))
      }
    }
  }

  test("RangeConstraints String properties") {
    val gen = for {
      min <- Gen.option(Gen.posNum[BigDecimal])
      max <- Gen.option(Gen.posNum[BigDecimal])
      input <- Gen.alphaNumStr
    } yield (min, max, input)
    forall(gen) { case (min, max, input) =>
      val hint = smithy.api.Range(min, max)
      val result = Constraints.RangeConstraints.checkString(Hints(hint))(input)
      expect(result == Right(()))
    }
  }

  test("RangeConstraints collection properties") {
    val gen = for {
      min <- Gen.option(Gen.posNum[BigDecimal])
      max <- Gen.option(Gen.posNum[BigDecimal])
      input <- Gen.listOf(Gen.long)
    } yield (min, max, input)
    forall(gen) { case (min, max, input) =>
      val hint = smithy.api.Range(min, max)
      val result =
        Constraints.RangeConstraints.checkCollection(Hints(hint))(input)
      expect(result == Right(()))
    }
  }

  test("UniqueItemsConstraints checks for duplicates in Lists - valid inputs") {
    forall(Gen.listOf(Gen.long).map(_.distinct)) { input =>
      val hint = UniqueItems()
      val result =
        Constraints.UniqueItemsConstraints.checkCollection(Hints(hint))(input)
      expect(result == Right(()))
    }
  }

  pureTest(
    "UniqueItemsConstraints checks for duplicates in Lists - invalid input"
  ) {
    val hint = UniqueItems()
    val result = Constraints.UniqueItemsConstraints.checkCollection(
      Hints(hint)
    )(List(1, 2, 3, 1))
    expect(
      result == Left(
        Constraints.ConstraintError(
          hint,
          "List contains duplicate items while marked with UniqueItems trait"
        )
      )
    )
  }

  test("UniqueItemsConstraints String properties") {
    forall(Gen.alphaNumStr) { input =>
      val hint = UniqueItems()
      val result =
        Constraints.UniqueItemsConstraints.checkString(Hints(hint))(input)
      expect(result == Right(()))
    }
  }

  test("UniqueItemsConstraints Numeric properties") {
    forall(Gen.long) { input =>
      val hint = UniqueItems()
      val f = Constraints.UniqueItemsConstraints.checkNumeric[Long](Hints(hint))
      expect(f(input) == Right(()))
    }
  }

  pureTest("Constraints composition") {
    val constraints =
      Constraints.PatternConstraints ++ Constraints.LengthConstraints
    val length = Length(Some(1), Some(5))
    val pattern = Pattern("\\w+")
    val hints = Hints(pattern, length)
    val check = constraints.checkString(hints)

    expect(
      check("!hello!") == Left(
        Constraints.ConstraintError(
          length,
          "length required to be >= 1 and <= 5, but was 7"
        )
      )
    ) &&
    expect(
      check("!!") == Left(
        Constraints.ConstraintError(
          pattern,
          "String '!!' does not match pattern '\\w+'"
        )
      )
    ) &&
    expect(
      check("!!!!!!") == Left(
        Constraints.ConstraintError(
          pattern,
          "String '!!!!!!' does not match pattern '\\w+'"
        )
      )
    ) // shows left side precedence of ++
  }

  private def runLengthTest[A: Show](
      getLength: A => Int,
      genA: Gen[A],
      check: Hints => Option[A => Either[Constraints.ConstraintError, Unit]]
  ): IO[Expectations] = {
    val gen = for {
      min <- Gen.option(Gen.posNum[Long])
      max <- Gen.option(Gen.posNum[Long])
      input <- genA
    } yield (min, max, input)
    forall(gen) { case (min, max, input) =>
      val testLength = getLength(input)
      val hints = Hints(Length(min, max))
      val result = check(hints)(input)
      result match {
        case Left(_) =>
          expect(min.exists(_ > testLength) || max.exists(_ < testLength))
        case Right(_) =>
          expect(min.forall(_ <= testLength) && max.forall(_ >= testLength))
      }
    }
  }

  private implicit class MaybeCheckOps[A](
      maybeCheck: Option[A => Either[Constraints.ConstraintError, Unit]]
  ) {
    def apply(a: A): Either[Constraints.ConstraintError, Unit] =
      maybeCheck match {
        case Some(check) => check(a)
        case None        => Right(())
      }
  }
}
