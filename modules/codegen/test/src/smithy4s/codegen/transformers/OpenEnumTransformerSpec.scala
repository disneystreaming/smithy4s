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

package smithy4s.codegen.transformers

import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import alloy.OpenEnumTrait

final class OpenEnumTransformerSpec extends munit.FunSuite {

  test("adds OpenEnum on shapes with enum trait") {
    val input =
      """|$version: "2"
         |
         |namespace com.amazonaws.kinesis
         |
         |@enum([
         |  {value: "TEST", name: "TEST"}
         |])
         |string Test
         |""".stripMargin

    runTest(input)
  }

  test("adds OpenEnum on intEnum shapes") {
    val input =
      """|$version: "2"
         |
         |namespace com.amazonaws.kinesis
         |
         |intEnum Test {
         |  ONE = 1
         |}
         |""".stripMargin

    runTest(input)
  }

  test("adds OpenEnum on enum shapes") {
    val input =
      """|$version: "2"
         |
         |namespace com.amazonaws.kinesis
         |
         |enum Test {
         |  ONE
         |}
         |""".stripMargin

    runTest(input)
  }

  test("DO NOT add OpenEnum on enum shapes outside of aws namespace") {
    val input =
      """|$version: "2"
         |
         |namespace test
         |
         |enum Test {
         |  ONE
         |}
         |""".stripMargin

    val transformedModel =
      new OpenEnumTransformer()
        .transform(
          TransformContext
            .builder()
            .model(loadModel(input))
            .build()
        )

    val containsOpenEnum = transformedModel
      .expectShape(ShapeId.fromParts("test", "Test"))
      .hasTrait(classOf[OpenEnumTrait])

    assert(!containsOpenEnum, "Expected openEnum trait NOT to be present")
  }

  private def runTest(inputModel: String)(implicit
      loc: munit.Location
  ): Unit = {
    val transformedModel =
      new OpenEnumTransformer()
        .transform(
          TransformContext
            .builder()
            .model(loadModel(inputModel))
            .build()
        )

    val containsOpenEnum = transformedModel
      .expectShape(ShapeId.fromParts("com.amazonaws.kinesis", "Test"))
      .hasTrait(classOf[OpenEnumTrait])

    assert(containsOpenEnum, "Expected openEnum trait to be present")
  }

  private def loadModel(strModels: String*): Model = {
    val assembler = Model
      .assembler()
      .disableValidation()
      .discoverModels()

    strModels
      .foldLeft(assembler) { case (a, model) =>
        a.addUnparsedModel(s"test-${model.hashCode}.smithy", model)
      }
      .assemble()
      .unwrap()
  }
}
