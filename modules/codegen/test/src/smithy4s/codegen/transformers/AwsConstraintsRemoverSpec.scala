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

import smithy4s.codegen.internals.TestUtils
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.shapes.ShapeId

import scala.jdk.CollectionConverters._

final class AwsConstraintsRemoverSpec extends munit.FunSuite {
  import smithy4s.codegen.internals.TestUtils._

  test("Remove length trait") {
    val example =
      """
        |$version: "2"
        |
        |namespace com.amazonaws.kinesis.service
        |
        |operation GetOutput {
        | output: com.amazonaws.kinesis#Long,
        |}
        |""".stripMargin
    val kinesis =
      """
        |$version: "2"
        |
        |namespace com.amazonaws.kinesis
        |
        |@range(min: 1, max: 10)
        |long Long
        |""".stripMargin
    val originalModel = loadModel(example, kinesis)
    val transformed = new AwsConstraintsRemover().transform(
      TransformContext.builder().model(originalModel).build()
    )

    val before = originalModel
      .expectShape(ShapeId.from("com.amazonaws.kinesis#Long"))
      .getAllTraits()
      .asScala
      .get(ShapeId.from("smithy.api#range"))

    val after = transformed
      .expectShape(ShapeId.from("com.amazonaws.kinesis#Long"))
      .getAllTraits()
      .asScala
      .get(ShapeId.from("smithy.api#range"))

    assert(before.isDefined)
    assert(after.isEmpty)

    val generatedCode = TestUtils.generateScalaCode(transformed)

    val actualCode = generatedCode("com.amazonaws.kinesis.Long")

    val expected =
      """|package com.amazonaws.kinesis
         |
         |import smithy4s.Hints
         |import smithy4s.Newtype
         |import smithy4s.Schema
         |import smithy4s.ShapeId
         |import smithy4s.schema.Schema.bijection
         |import smithy4s.schema.Schema.long
         |
         |object Long extends Newtype[scala.Long] {
         |  val id: ShapeId = ShapeId("com.amazonaws.kinesis", "Long")
         |  val hints: Hints = Hints(
         |    smithy.api.Box(),
         |  )
         |  val underlyingSchema: Schema[scala.Long] = long.withId(id).addHints(hints)
         |  implicit val schema: Schema[Long] = bijection(underlyingSchema, asBijection)
         |}
         |""".stripMargin

    assertEquals(actualCode, expected)
  }

}
