package smithy4s.internals

import munit._
import smithy4s._
import smithy4s.schema.Schema._

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
    runDecode("__{one}-{two}$$", "__blah-101$$", Right(s))
    runDecode("{one}-{two}$$", "blah-101$$", Right(s))
    runDecode("__{one}-{two}", "__blah-101", Right(s))
    runDecode("{one}-++-{two}", "blah-++-101", Right(s))
    runDecode("(({one}))(({two}))", "((blah))((101))", Right(s))
  }

  private def runEncode(pattern: String, input: TestStruct, expect: String)(
      implicit loc: Location
  ): Unit = {
    val result = StructurePatternRefinementProvider
      .provider[TestStruct]
      .make(
        alloy
          .StructurePattern(
            pattern = pattern,
            target = "test#TestStruct"
          )
      )
      .from(input)

    assertEquals(expect, result)
  }

  private def runDecode(
      pattern: String,
      input: String,
      expect: Either[String, TestStruct]
  )(implicit
      loc: Location
  ): Unit = {
    val result = StructurePatternRefinementProvider
      .provider[TestStruct]
      .make(
        alloy
          .StructurePattern(
            pattern = pattern,
            target = "test#TestStruct"
          )
      )
      .apply(input)

    assertEquals(expect, result)
  }

}
