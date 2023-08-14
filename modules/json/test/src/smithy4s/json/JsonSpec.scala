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

package smithy4s.json

import munit._
import smithy4s.schema.Schema
import smithy4s.Blob
import smithy4s.Document
import Schema._
import smithy4s.Removable

class JsonSpec() extends FunSuite {

  case class Foo(a: Int, b: Option[Int])
  object Foo {
    implicit val schema: Schema[Foo] = {
      val a = int.required[Foo]("a", _.a)
      val b = int.optional[Foo]("b", _.b)
      struct(a, b)(Foo.apply)
    }
  }

  test("Json read/write") {
    val foo = Foo(1, Some(2))
    val result = Json.writePrettyString(foo)
    val roundTripped = Json.read[Foo](Blob(result))
    val expectedJson = """|{
                          |  "a": 1,
                          |  "b": 2
                          |}""".stripMargin

    assertEquals(result, expectedJson)
    assertEquals(roundTripped, Right(foo))
  }

  test("Json document read/write") {
    val foo =
      Document.obj("a" -> Document.fromInt(1), "b" -> Document.fromInt(2))
    val result = Json.writeDocumentAsPrettyString(foo)
    val roundTripped = Json.readDocument(Blob(result))
    val expectedJson = """|{
                          |  "a": 1,
                          |  "b": 2
                          |}""".stripMargin

    assertEquals(result, expectedJson)
    assertEquals(roundTripped, Right(foo))
  }

  case class Patchable(a: Removable[Int])
  object Patchable {
    implicit val schema: Schema[Patchable] = {
      val a = Removable.schema(int).required[Patchable]("a", _.a)
      struct(a)(Patchable.apply)
    }
  }

  test("Json patchable") {
    val patchable = Patchable(Removable.Removed)
    val result = Json.writePrettyString(patchable)
    val expectedJson = """|{
                          |  "a": null
                          |}""".stripMargin
    val roundTripped = Json.read[Patchable](Blob(result))
    assertEquals(result, expectedJson)
    assertEquals(roundTripped, Right(patchable))
  }

}
