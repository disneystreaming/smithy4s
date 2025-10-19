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

import munit.Location

final class DynamicHintRenderingSpec extends munit.FunSuite {

  test("dynamic hint rendering mode - structure") {
    val smithySpec =
      """|$version: "2.0"
         |
         |namespace test
         |
         |@trait
         |@smithy4s.meta#renderAsDynamicBinding
         |structure someTrait {
         |  @required
         |  int: Integer
         |}
         |
         |@someTrait(int: 1)
         |structure Test {
         | @someTrait(int: 2)
         | one: String
         |}
         |
         |""".stripMargin

    runTest(smithySpec)
  }

  test("dynamic hint rendering mode - enum") {
    val smithySpec = """|$version: "2.0"
                        |
                        |namespace test
                        |
                        |@trait
                        |@smithy4s.meta#renderAsDynamicBinding
                        |structure someTrait {
                        |  @required
                        |  int: Integer
                        |}
                        |
                        |@someTrait(int: 1)
                        |enum Test {
                        | @someTrait(int: 2)
                        | ONE = "one"
                        |}
                        |
                        |""".stripMargin

    runTest(smithySpec)
  }

  test("dynamic hint rendering mode - union") {
    val smithySpec = """|$version: "2.0"
                        |
                        |namespace test
                        |
                        |@trait
                        |@smithy4s.meta#renderAsDynamicBinding
                        |structure someTrait {
                        |  @required
                        |  int: Integer
                        |}
                        |
                        |@someTrait(int: 1)
                        |union Test {
                        | @someTrait(int: 2)
                        | one: String
                        | two: Integer
                        |}
                        |
                        |""".stripMargin

    runTest(smithySpec)
  }

  test("dynamic hint rendering mode - primitive") {
    val smithySpec = """|$version: "2.0"
                        |
                        |namespace test
                        |
                        |@trait
                        |@smithy4s.meta#renderAsDynamicBinding
                        |structure someTrait {
                        |  @required
                        |  int: Integer
                        |}
                        |
                        |@someTrait(int: 1)
                        |string Test
                        |
                        |@someTrait(int: 2)
                        |integer Other
                        |""".stripMargin

    runTest(smithySpec)
  }

  test("dynamic hint rendering mode - service") {
    val smithySpec = """|$version: "2.0"
                        |
                        |namespace test
                        |
                        |@trait
                        |@smithy4s.meta#renderAsDynamicBinding
                        |structure someTrait {
                        |  @required
                        |  int: Integer
                        |}
                        |
                        |@someTrait(int: 1)
                        |service Test {
                        |  operations: [One]
                        |}
                        |
                        |@someTrait(int: 2)
                        |operation One {}
                        |
                        |""".stripMargin

    runTest(smithySpec)
  }

  private def runTest(smithySpec: String)(implicit loc: Location): Unit = {
    val expect = List(
      """Hints.dynamic(ShapeId("test", "someTrait"), smithy4s.Document.obj("int" -> smithy4s.Document.fromDouble(1.0d)))""",
      """Hints.dynamic(ShapeId("test", "someTrait"), smithy4s.Document.obj("int" -> smithy4s.Document.fromDouble(2.0d)))"""
    )

    val result = TestUtils.generateScalaCode(smithySpec).values.toList

    TestUtils.assertContainsSection(
      files = result,
      expectedSections = expect
    )
  }

}
