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

package smithy4s.codegen.internals

final class DefaultRenderModeSpec extends munit.FunSuite {

  test("default render mode - NONE") {
    val smithy = """|$version: "2.0"
                    |
                    |metadata smithy4sDefaultRenderMode = "NONE"
                    |
                    |namespace foo
                    |
                    |structure Test {
                    |  one: String
                    |  two: String = "test"
                    |  @required
                    |  three: String
                    |}
                    |""".stripMargin

    val scalaCode =
      """|package foo
         |
         |import smithy4s.Schema
         |import smithy4s.Hints
         |import smithy4s.schema.Schema.string
         |import smithy4s.ShapeId
         |import smithy4s.schema.Schema.struct
         |import smithy4s.ShapeTag
         |
         |case class Test(one: Option[String], two: String, three: String)
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("foo", "Test")
         |
         |  val hints : Hints = Hints.empty
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.optional[Test]("one", _.one),
         |    string.required[Test]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.required[Test]("three", _.three).addHints(smithy.api.Required()),
         |  ){
         |    Test.apply
         |  }.withId(id).addHints(hints)
         |}""".stripMargin

    TestUtils.runTest(smithy, scalaCode)
  }

  test("default render mode - OPTION_ONLY") {
    val smithy = """|$version: "2.0"
                    |
                    |metadata smithy4sDefaultRenderMode = "OPTION_ONLY"
                    |
                    |namespace foo
                    |
                    |structure Test {
                    |  one: String
                    |  two: String = "test"
                    |  @required
                    |  three: String
                    |}
                    |""".stripMargin

    val scalaCode =
      """|package foo
         |
         |import smithy4s.Schema
         |import smithy4s.Hints
         |import smithy4s.schema.Schema.string
         |import smithy4s.ShapeId
         |import smithy4s.schema.Schema.struct
         |import smithy4s.ShapeTag
         |
         |case class Test(two: String, three: String, one: Option[String] = None)
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("foo", "Test")
         |
         |  val hints : Hints = Hints.empty
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.required[Test]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.required[Test]("three", _.three).addHints(smithy.api.Required()),
         |    string.optional[Test]("one", _.one),
         |  ){
         |    Test.apply
         |  }.withId(id).addHints(hints)
         |}""".stripMargin

    TestUtils.runTest(smithy, scalaCode)
  }

  test("default render mode - FULL") {
    val smithy = """|$version: "2.0"
                    |
                    |metadata smithy4sDefaultRenderMode = "FULL"
                    |
                    |namespace foo
                    |
                    |structure Test {
                    |  one: String
                    |  two: String = "test"
                    |  @required
                    |  three: String
                    |}
                    |""".stripMargin

    val scalaCode =
      """|package foo
         |
         |import smithy4s.Schema
         |import smithy4s.Hints
         |import smithy4s.schema.Schema.string
         |import smithy4s.ShapeId
         |import smithy4s.schema.Schema.struct
         |import smithy4s.ShapeTag
         |
         |case class Test(three: String, two: String = "test", one: Option[String] = None)
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("foo", "Test")
         |
         |  val hints : Hints = Hints.empty
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.required[Test]("three", _.three).addHints(smithy.api.Required()),
         |    string.required[Test]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.optional[Test]("one", _.one),
         |  ){
         |    Test.apply
         |  }.withId(id).addHints(hints)
         |}""".stripMargin

    TestUtils.runTest(smithy, scalaCode)
  }

}
