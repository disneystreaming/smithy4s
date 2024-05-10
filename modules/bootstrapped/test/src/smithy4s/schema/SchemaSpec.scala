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
package schema

import munit._
import Schema._
import smithy.api.Default
import smithy.api.Documentation

final class SchemaSpec extends FunSuite {

  test("getDefault - some") {
    val sch = string.addHints(Default(Document.fromString("test")))

    assertEquals(sch.getDefault, Some(Document.fromString("test")))
    assertEquals(sch.getDefaultValue, Some("test"))
  }

  test("getDefault - none") {
    val sch = string

    assertEquals(sch.getDefault, None)
    assertEquals(sch.getDefaultValue, None)
  }

  test("bijection - identity") {
    val sut = Bijection.identity[String]
    val str = "value"
    assertEquals(sut.to(sut.from(str)), str)
  }

  test("Hints can be added to fields as varars or full Hints") {
    case class Test(foo: String)
    val documentation = Documentation("hello")
    val getter = (_: Test).foo
    val field1 =
      Schema.string.required[Test]("foo", getter).addHints(documentation)
    val field2 =
      Schema.string.required[Test]("foo", getter).addHints(Hints(documentation))
    assertEquals(field1, field2)
  }

}
