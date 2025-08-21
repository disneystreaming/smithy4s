/*
 *  Copyright 2021-2025 Disney Streaming
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

final class DynamicHintRenderingSpec extends munit.FunSuite {

  test("dynamic hint rendering mode") {
    val smithySpec = """|$version: "2.0"
                        |
                        |metadata smithy4sRenderDynamicHintBindings = true
                        |
                        |namespace test
                        |
                        |@input
                        |structure Test {
                        | @required
                        | @jsonName("field_one")
                        | @httpHeader("x-one")
                        | one: String
                        |
                        | @timestampFormat("date-time")
                        | two: Timestamp
                        |}
                        |
                        |""".stripMargin

    val scalaCode =
      """|package test
         |
         |import smithy4s.Hints
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.Timestamp
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.struct
         |import smithy4s.schema.Schema.timestamp
         |
         |final case class Test(one: String, two: Option[Timestamp] = None)
         |
         |object Test extends ShapeTag.Companion[Test] {
         |  val id: ShapeId = ShapeId("test", "Test")
         |
         |  val hints: Hints = Hints(
         |    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
         |  ).lazily
         |
         |  // constructor using the original order from the spec
         |  private def make(one: String, two: Option[Timestamp]): Test = Test(one, two)
         |
         |  implicit val schema: Schema[Test] = struct(
         |    string.required[Test]("one", _.one).addHints(Hints.dynamic(ShapeId("smithy.api", "httpHeader"), smithy4s.Document.fromString("x-one")), Hints.dynamic(ShapeId("smithy.api", "jsonName"), smithy4s.Document.fromString("field_one"))),
         |    timestamp.optional[Test]("two", _.two).addHints(Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("date-time"))),
         |  )(make).withId(id).addHints(hints)
         |}
         |""".stripMargin

    TestUtils.runTest(smithySpec, scalaCode)
  }

}
