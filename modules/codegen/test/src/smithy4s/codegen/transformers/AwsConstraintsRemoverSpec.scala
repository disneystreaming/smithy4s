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

  test("Remove length, range, pattern trait") {
    val dummyServices =
      """
        |$version: "2"
        |
        |namespace com.amazonaws.dummy.service
        |
        |operation GetLong {
        | output: com.amazonaws.dummy#Long,
        |}
        |operation GetStringChecked {
        | output: com.amazonaws.dummy#StringChecked,
        |}
        |operation GetStringLength {
        | output: com.amazonaws.dummy#StringLength,
        |}
        |""".stripMargin
    val dummy =
      """
        |$version: "2"
        |
        |namespace com.amazonaws.dummy
        |
        |@range(min: 1, max: 10)
        |long Long
        |
        |@pattern("^[A-Za-z]+$")
        |string StringChecked
        |
        |@length(min: 1, max: 10)
        |string StringLength
        |""".stripMargin
    val originalModel = loadModel(dummyServices, dummy)
    val transformed = new AwsConstraintsRemover().transform(
      TransformContext.builder().model(originalModel).build()
    )

    Seq(
      "com.amazonaws.dummy#Long" -> "smithy.api#range",
      "com.amazonaws.dummy#StringChecked" -> "smithy.api#pattern",
      "com.amazonaws.dummy#StringLength" -> "smithy.api#length"
    ).foreach { case (sId, tId) =>
      val before = originalModel
        .expectShape(ShapeId.from(sId))
        .getAllTraits()
        .asScala
        .get(ShapeId.from(tId))

      val after = transformed
        .expectShape(ShapeId.from(sId))
        .getAllTraits()
        .asScala
        .get(ShapeId.from(tId))

      assert(
        before.isDefined,
        s"The trait $tId was not defined on $sId before the transformation"
      )
      assert(
        after.isEmpty,
        s"The trait $tId was defined on $sId after the transformation"
      )
    }

    val generatedCode = TestUtils.generateScalaCode(transformed)
    println(generatedCode.keySet)

    Seq(
      "com.amazonaws.dummy.Long" -> """|package com.amazonaws.dummy
                                       |
                                       |import smithy4s.Hints
                                       |import smithy4s.Newtype
                                       |import smithy4s.Schema
                                       |import smithy4s.ShapeId
                                       |import smithy4s.schema.Schema.bijection
                                       |import smithy4s.schema.Schema.long
                                       |
                                       |object Long extends Newtype[scala.Long] {
                                       |  val id: ShapeId = ShapeId("com.amazonaws.dummy", "Long")
                                       |  val hints: Hints = Hints(
                                       |    smithy.api.Box(),
                                       |  )
                                       |  val underlyingSchema: Schema[scala.Long] = long.withId(id).addHints(hints)
                                       |  implicit val schema: Schema[Long] = bijection(underlyingSchema, asBijection)
                                       |}
                                       |""".stripMargin,
      "com.amazonaws.dummy.StringChecked" -> """|package com.amazonaws.dummy
                                                |
                                                |import smithy4s.Hints
                                                |import smithy4s.Newtype
                                                |import smithy4s.Schema
                                                |import smithy4s.ShapeId
                                                |import smithy4s.schema.Schema.bijection
                                                |import smithy4s.schema.Schema.string
                                                |
                                                |object StringChecked extends Newtype[String] {
                                                |  val id: ShapeId = ShapeId("com.amazonaws.dummy", "StringChecked")
                                                |  val hints: Hints = Hints.empty
                                                |  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
                                                |  implicit val schema: Schema[StringChecked] = bijection(underlyingSchema, asBijection)
                                                |}
                                                |""".stripMargin,
      "com.amazonaws.dummy.StringLength" -> """|package com.amazonaws.dummy
                                               |
                                               |import smithy4s.Hints
                                               |import smithy4s.Newtype
                                               |import smithy4s.Schema
                                               |import smithy4s.ShapeId
                                               |import smithy4s.schema.Schema.bijection
                                               |import smithy4s.schema.Schema.string
                                               |
                                               |object StringLength extends Newtype[String] {
                                               |  val id: ShapeId = ShapeId("com.amazonaws.dummy", "StringLength")
                                               |  val hints: Hints = Hints.empty
                                               |  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
                                               |  implicit val schema: Schema[StringLength] = bijection(underlyingSchema, asBijection)
                                               |}
                                               |""".stripMargin
    ).foreach { case (sId, expected) =>
      val actualCode = generatedCode(sId)
      assertEquals(actualCode, expected)
    }
  }

}
