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

package smithy4s.codegen.transformers

import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import smithy4s.codegen.internals.TestUtils

final class AwsConstraintsRemoverSpec extends munit.FunSuite {

  test("Remove length trait") {
    val example =
      """
        |$version: "2"
        |
        |namespace test
        |
        |
        |operation GetOutput {
        | output: com.amazonaws.kinesis#Long,
        |}
        |""".stripMargin
    val transformed = new AwsConstraintsRemover().transform(
      TransformContext.builder().model(ll(example)).build()
    )

    val code =
      TestUtils.generateScalaCode(transformed)("test.TestOutput")

    val expected =
      """|package test
         |
         |import smithy4s.Hints
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.ShapeTag
         |import smithy4s.schema.Schema.string
         |import smithy4s.schema.Schema.struct
         |
         |final case class TestOutput(o: Option[String] = None)
         |object TestOutput extends ShapeTag.Companion[TestOutput] {
         |  val id: ShapeId = ShapeId("test", "TestOutput")
         |
         |  val hints: Hints = Hints.empty
         |
         |  implicit val schema: Schema[TestOutput] = struct(
         |    string.optional[TestOutput]("o", _.o),
         |  ){
         |    TestOutput.apply
         |  }.withId(id).addHints(hints)
         |}
      """.stripMargin

    assertEquals(code, expected)
  }
  private def ll(namespaces: String*): Model = {
    val assembler = Model
      .assembler()
      .disableValidation()
      .discoverModels()

    namespaces
      .foldLeft(assembler) { case (a, model) =>
        a.addUnparsedModel(s"test-${model.hashCode}.smithy", model)
      }
      .assemble()
      .unwrap()
  }

}
