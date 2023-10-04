/*
 *  Copyright 2021-2023 Disney Streaming
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

package smithy4s.http.internals

import munit._

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
