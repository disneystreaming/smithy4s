/*
 *  Copyright 2021-2022 Disney Streaming
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

package smithy4s.internals

import munit._
import smithy4s._
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema._
import java.util.UUID
import smithy4s.example.OpenEnumTest

final class StructurePatternRefinementProviderSpec extends FunSuite {

  case class TestStruct(one: String, two: Int)
  object TestStruct {
    implicit val schema: Schema[TestStruct] =
      struct(
        string.required[TestStruct]("one", _.one),
        int.required[TestStruct]("two", _.two)
      )(TestStruct.apply)
  }

  test("encoding") {
    val s = TestStruct("blah", 101)
    runEncode("__{one}-{two}$$", s, "__blah-101$$")
    runEncode("{one}-{two}$$", s, "blah-101$$")
    runEncode("__{one}-{two}", s, "__blah-101")
    runEncode("{one}-++-{two}", s, "blah-++-101")
    runEncode("(({one}))(({two}))", s, "((blah))((101))")
  }

  test("decoding") {
    val s = TestStruct("blah", 101)
    runDecode("__{one}-{two}$$", "__blah-101$$", s)
    runDecode("{one}-{two}$$", "blah-101$$", s)
    runDecode("__{one}-{two}", "__blah-101", s)
    runDecode("{one}-++-{two}", "blah-++-101", s)
    runDecode("(({one}))(({two}))", "((blah))((101))", s)
    runDecode("{two}-{one}", "101-blah", s)
    // negative number (-101)
    runDecode("{one}-{two}", "blah--101", s.copy(two = -101))
    runDecode("{one}${two}", "blah$$101", s, shouldFail = true)
    runDecode("{one}-{two}", "bl-ah-101", s, shouldFail = true)
    runDecode("{one}-{two}", "blah-", s, shouldFail = true)
    runDecode("{one}-{two}", "blah", s, shouldFail = true)
    runDecode("{one}-{two}", "-101", s, shouldFail = true)
    runDecode("{one}-{two}", "101-blah", s, shouldFail = true)
  }

  sealed abstract class SomeEnum(
      _value: String,
      _name: String,
      _intValue: Int,
      _hints: Hints
  ) extends Enumeration.Value {
    override type EnumType = SomeEnum
    override val value: String = _value
    override val name: String = _name
    override val intValue: Int = _intValue
    override val hints: Hints = _hints

    override def enumeration: Enumeration[EnumType] = SomeEnum

    @inline final def widen: SomeEnum = this
  }
  object SomeEnum
      extends Enumeration[SomeEnum]
      with ShapeTag.Companion[SomeEnum] {
    case object ONE extends SomeEnum("ONE", "ONE", 1, Hints())
    case object TWO extends SomeEnum("TWO", "TWO", 2, Hints())

    val id: ShapeId = ShapeId("test", "SomeEnum")

    val hints: Hints = Hints()

    val values = List(ONE, TWO)

    val tag = EnumTag.ClosedStringEnum

    implicit val schema: Schema[SomeEnum] =
      enumeration(tag, values).withId(id).addHints(hints)
  }

  case class Primitives(
      a: Short,
      b: Int,
      c: Float,
      d: Long,
      e: Double,
      f: BigInt,
      g: BigDecimal,
      h: Boolean,
      i: String,
      j: UUID,
      k: Byte,
      l: Timestamp,
      m: SomeEnum,
      n: OpenEnumTest
  )

  object Primitives {
    implicit val schema: Schema[Primitives] =
      struct(
        short.required[Primitives]("a", _.a),
        int.required[Primitives]("b", _.b),
        float.required[Primitives]("c", _.c),
        long.required[Primitives]("d", _.d),
        double.required[Primitives]("e", _.e),
        bigint.required[Primitives]("f", _.f),
        bigdecimal.required[Primitives]("g", _.g),
        boolean.required[Primitives]("h", _.h),
        string.required[Primitives]("i", _.i),
        uuid.required[Primitives]("j", _.j),
        byte.required[Primitives]("k", _.k),
        timestamp.required[Primitives]("l", _.l),
        SomeEnum.schema.required[Primitives]("m", _.m),
        OpenEnumTest.schema.required[Primitives]("n", _.n)
      )(Primitives.apply)
  }

  test("primitives") {
    val in = Primitives(
      1,
      2,
      3f,
      4,
      5,
      BigInt(6),
      BigDecimal(7),
      true,
      "something",
      UUID.fromString("246365e6-1665-488a-9ec8-4cc916dc88f6"),
      'a'.toByte,
      Timestamp(0, 0),
      SomeEnum.ONE,
      OpenEnumTest.$Unknown("test")
    )
    val pattern = "{a}_{b}_{c}_{d}_{e}_{f}_{g}_{h}_{i}_{j}_{k}_{l}_{m}_{n}"
    val expect =
      if (Platform.isJS)
        "1_2_3_4_5_6_7_true_something_246365e6-1665-488a-9ec8-4cc916dc88f6_97_1970-01-01T00:00:00Z_ONE_test"
      else
        "1_2_3.0_4_5.0_6_7_true_something_246365e6-1665-488a-9ec8-4cc916dc88f6_97_1970-01-01T00:00:00Z_ONE_test"
    runEncode(pattern, in, expect)
  }

  private def runEncode[A](pattern: String, input: A, expect: String)(implicit
      sch: Schema[A],
      loc: Location
  ): Unit = {
    val result = StructurePatternRefinementProvider
      .provider[A]
      .make(
        alloy
          .StructurePattern(
            pattern = pattern,
            target = ShapeId("", "")
          )
      )
      .from(input)

    assertEquals(expect, result)

    val roundTrip = decode(pattern, result)

    assertEquals(input, roundTrip)
  }

  private def decode[A](
      pattern: String,
      input: String
  )(implicit sch: Schema[A]): A = {
    StructurePatternRefinementProvider
      .provider[A]
      .make(
        alloy
          .StructurePattern(
            pattern = pattern,
            target = ShapeId("", "")
          )
      )
      .apply(input)
      .toOption
      .get
  }

  private def runDecode[A](
      pattern: String,
      input: String,
      expect: A,
      shouldFail: Boolean = false
  )(implicit sch: Schema[A], loc: Location): Unit = {
    val result = util.Try {
      decode(pattern, input)
    }

    if (shouldFail) assert(result.isFailure)
    else assertEquals(expect, result.toOption.get)
  }

}
