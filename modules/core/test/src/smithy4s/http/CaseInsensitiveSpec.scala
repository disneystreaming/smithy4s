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
package http

class CaseInsensitiveSpec() extends munit.FunSuite {

  val foo = CaseInsensitive("Foo")

  test("equality") {
    expect.same(CaseInsensitive("Foo"), CaseInsensitive("foo"))
  }

  test("equality") {
    val sorted = List(CaseInsensitive("Z"), CaseInsensitive("a")).sorted
    expect.same(sorted, List(CaseInsensitive("A"), CaseInsensitive("z")))
  }

  test("data retention") {
    expect.same(CaseInsensitive("FoO").toString, "FoO")
  }

  test("starts with") {
    val FOObar = CaseInsensitive("FOObar")
    expect(FOObar.startsWith("foo"))
  }

}
