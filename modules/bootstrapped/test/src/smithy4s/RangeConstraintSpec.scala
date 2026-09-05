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

package smithy4s

import munit._
import smithy.api.Range

class RangeConstraintSpec extends FunSuite {

  private def check[N: Numeric](range: Range, value: N): Either[String, N] =
    RefinementProvider.numericRangeConstraints[N].make(range).apply(value)

  test("long above a max that is not representable as a double is rejected") {
    val range = Range(min = None, max = Some(BigDecimal(9007199254740992L)))
    assertEquals(
      check(range, 9007199254740993L),
      Left("Input must be <= 9007199254740992, but was 9007199254740993")
    )
  }

  test(
    "long equal to a min that is not representable as a double is accepted"
  ) {
    val range = Range(min = Some(BigDecimal(9007199254740993L)), max = None)
    assertEquals(check(range, 9007199254740993L), Right(9007199254740993L))
  }

  test("long bounds are compared exactly") {
    val range = Range(
      min = Some(BigDecimal(Long.MinValue)),
      max = Some(BigDecimal(Long.MaxValue))
    )
    assertEquals(check(range, Long.MaxValue), Right(Long.MaxValue))
    assertEquals(check(range, Long.MinValue), Right(Long.MinValue))
    assertEquals(
      check(
        Range(min = None, max = Some(BigDecimal(Long.MaxValue) - 1)),
        Long.MaxValue
      ),
      Left(s"Input must be <= ${Long.MaxValue - 1}, but was ${Long.MaxValue}")
    )
  }

  test("big integers beyond the double range are accepted") {
    val huge = BigInt(10).pow(400)
    val range = Range(min = Some(BigDecimal(0)), max = None)
    assertEquals(check(range, huge), Right(huge))
    assertEquals(
      check(Range(min = None, max = Some(BigDecimal(huge - 1))), huge),
      Left(s"Input must be <= ${BigDecimal(huge - 1)}, but was $huge")
    )
  }

  test("big decimals are compared exactly") {
    val value = BigDecimal("0.10000000000000000000000000000000001")
    val range = Range(min = None, max = Some(BigDecimal("0.1")))
    assertEquals(
      check(range, value),
      Left(s"Input must be <= 0.1, but was $value")
    )
    assertEquals(
      check(Range(min = Some(BigDecimal("0.1")), max = None), value),
      Right(value)
    )
  }

  test("ints and doubles are still validated") {
    val range = Range(min = Some(BigDecimal(1)), max = Some(BigDecimal(10)))
    assertEquals(check(range, 5), Right(5))
    assertEquals(
      check(range, 11),
      Left("Input must be >= 1 and <= 10, but was 11")
    )
    assertEquals(check(range, 2.5), Right(2.5))
    assertEquals(
      check(range, 10.5),
      Left("Input must be >= 1 and <= 10, but was 10.5")
    )
    assertEquals(
      check(range, 0.5f),
      Left("Input must be >= 1 and <= 10, but was 0.5")
    )
  }

  test("floats are compared using their decimal representation") {
    val range =
      Range(min = Some(BigDecimal("0.1")), max = Some(BigDecimal("0.3")))
    assertEquals(check(range, 0.1f), Right(0.1f))
    assertEquals(check(range, 0.3f), Right(0.3f))
    assertEquals(
      check(range, 0.4f),
      Left("Input must be >= 0.1 and <= 0.3, but was 0.4")
    )
  }

  test("NaN and infinities are rejected") {
    val range = Range(min = Some(BigDecimal(1)), max = None)
    assert(check(range, Double.NaN).isLeft)
    assert(check(range, Double.PositiveInfinity).isLeft)
    assert(check(range, Float.NegativeInfinity).isLeft)
  }
}
