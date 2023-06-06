package smithy4s.http.internals

import munit._
import smithy4s.http.internals.SchemaVisitorHeaderSplit

class HeaderSplitSpec extends FunSuite {

  test("Header split: single value") {
    checkNormal("abcd efgh")("abcd efgh")
  }

  test("Header split: multiple normal strings") {
    checkNormal("a, b, c")("a", "b", "c")
  }

  test("Header split: quoted strings") {
    checkNormal("\"b,c\", \"\\\"def\\\"\", a")("b,c", "\"def\"", "a")
  }

  test("Header split: http-date timestamps") {
    checkHttpDates(
      "Mon, 16 Dec 2019 23:48:18 GMT, Mon, 16 Dec 2019 23:48:18 GMT"
    )("Mon, 16 Dec 2019 23:48:18 GMT", "Mon, 16 Dec 2019 23:48:18 GMT")
  }

  def checkNormal(input: String)(output: String*)(implicit
      loc: Location
  ): Unit =
    assertEquals(
      SchemaVisitorHeaderSplit.splitHeaderValue(input, false),
      output
    )

  def checkHttpDates(input: String)(output: String*)(implicit
      loc: Location
  ): Unit =
    assertEquals(
      SchemaVisitorHeaderSplit.splitHeaderValue(input, true),
      output
    )

}
