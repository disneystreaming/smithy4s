/*
 *  Copyright 2021-2024 Disney Streaming
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
                    |use alloy#nullable
                    |
                    |structure Test {
                    |  one: String
                    |  two: String = "test"
                    |  @required
                    |  three: String
                    |  @required
                    |  four: String = "test"
                    |  @nullable
                    |  five: String
                    |  @nullable
                    |  six: String = "test"
                    |  @nullable
                    |  seven: String = null
                    |  @nullable
                    |  @required
                    |  eight: String
                    |}
                    |""".stripMargin

    val scalaCode =
      """|package foo
         |
         |import smithy4s.Hints
         |import smithy4s.Nullable
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.struct
         |
         |final case class Test(one: Option[String], two: String, three: String, four: String, five: Option[Nullable[String]], six: Nullable[String], seven: Nullable[String], eight: Nullable[String])
         |
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("foo", "Test")
         |
         |  val hints: Hints = Hints.empty
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.optional[Test]("one", _.one),
         |    string.field[Test]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.required[Test]("three", _.three),
         |    string.required[Test]("four", _.four).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.nullable.optional[Test]("five", _.five),
         |    string.nullable.field[Test]("six", _.six).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.nullable.field[Test]("seven", _.seven).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
         |    string.nullable.required[Test]("eight", _.eight),
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
                    |use alloy#nullable
                    |
                    |structure Test {
                    |  one: String
                    |  two: String = "test"
                    |  @required
                    |  three: String
                    |  @required
                    |  four: String = "test"
                    |  @nullable
                    |  five: String
                    |  @nullable
                    |  six: String = "test"
                    |  @nullable
                    |  seven: String = null
                    |  @nullable
                    |  @required
                    |  eight: String
                    |}
                    |""".stripMargin

    val scalaCode =
      """|package foo
         |
         |import smithy4s.Hints
         |import smithy4s.Nullable
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.struct
         |
         |final case class Test(two: String, three: String, four: String, six: Nullable[String], seven: Nullable[String], eight: Nullable[String], one: Option[String] = None, five: Option[Nullable[String]] = None)
         |
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("foo", "Test")
         |
         |  val hints: Hints = Hints.empty
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.field[Test]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.required[Test]("three", _.three),
         |    string.required[Test]("four", _.four).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.nullable.field[Test]("six", _.six).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.nullable.field[Test]("seven", _.seven).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
         |    string.nullable.required[Test]("eight", _.eight),
         |    string.optional[Test]("one", _.one),
         |    string.nullable.optional[Test]("five", _.five),
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
                    |use alloy#nullable
                    |
                    |structure Test {
                    |  one: String
                    |  two: String = "test"
                    |  @required
                    |  three: String
                    |  @required
                    |  four: String = "test"
                    |  @nullable
                    |  five: String
                    |  @nullable
                    |  six: String = "test"
                    |  @nullable
                    |  seven: String = null
                    |  @nullable
                    |  @required
                    |  eight: String
                    |}
                    |""".stripMargin

    val scalaCode =
      """|package foo
         |
         |import smithy4s.Hints
         |import smithy4s.Nullable
         |import smithy4s.Nullable.Null
         |import smithy4s.Nullable.Value
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.struct
         |
         |final case class Test(three: String, eight: Nullable[String], two: String = "test", four: String = "test", six: Nullable[String] = Value("test"), seven: Nullable[String] = Null, one: Option[String] = None, five: Option[Nullable[String]] = None)
         |
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("foo", "Test")
         |
         |  val hints: Hints = Hints.empty
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.required[Test]("three", _.three),
         |    string.nullable.required[Test]("eight", _.eight),
         |    string.field[Test]("two", _.two).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.required[Test]("four", _.four).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.nullable.field[Test]("six", _.six).addHints(smithy.api.Default(smithy4s.Document.fromString("test"))),
         |    string.nullable.field[Test]("seven", _.seven).addHints(smithy.api.Default(smithy4s.Document.nullDoc)),
         |    string.optional[Test]("one", _.one),
         |    string.nullable.optional[Test]("five", _.five),
         |  ){
         |    Test.apply
         |  }.withId(id).addHints(hints)
         |}
         |""".stripMargin

    TestUtils.runTest(smithy, scalaCode)
  }
}
