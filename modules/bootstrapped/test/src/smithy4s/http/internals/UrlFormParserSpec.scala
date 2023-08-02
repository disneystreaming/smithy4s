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

package smithy4s
package http
package internals

import smithy4s.codecs.PayloadPath

class UrlFormParserSpec() extends munit.FunSuite {

  test("struct") {
    val expected = UrlForm(
      List(
        UrlForm.FormData(path = PayloadPath("x"), maybeValue = Some("value-x")),
        UrlForm.FormData(path = PayloadPath("y"), maybeValue = Some("value-y"))
      )
    )

    checkContent(expected, "x=value-x&y=value-y")
  }

  test("list") {
    val expected = UrlForm(
      List(
        UrlForm.FormData(
          path = PayloadPath("foos", "member", 1),
          maybeValue = Some("1")
        ),
        UrlForm.FormData(
          path = PayloadPath("foos", "member", 2),
          maybeValue = Some("2")
        ),
        UrlForm.FormData(
          path = PayloadPath("foos", "member", 3),
          maybeValue = Some("3")
        )
      )
    )

    checkContent(expected, "foos.member.1=1&foos.member.2=2&foos.member.3=3")
  }

  def checkContent[A](expected: UrlForm, urlFormString: String)(implicit
      loc: munit.Location
  ): Unit = {
    val result = UrlFormParser.parseUrlForm(urlFormString)
    expect.same(result, Right(expected))
  }
}
