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

package smithy4s.codegen.transformers

import software.amazon.smithy.build.TransformContext
import smithy4s.meta.ValidateNewtypeTrait
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.Model

class ValidatedNewtypesTransformerSpec extends munit.FunSuite {

  import smithy4s.codegen.internals.TestUtils._

  test(
    "Leaves shape unchanged when @validateNewtype is already present"
  ) {
    assertPresent("smithy4s.transformer.test#ValidatedString") {
      """|$version: "2.0"
         |
         |metadata smithy4sRenderValidatedNewtypes = false
         |
         |namespace smithy4s.transformer.test
         |
         |use smithy4s.meta#validateNewtype
         |
         |@length(min: 1, max: 10)
         |@validateNewtype
         |string ValidatedString
         |""".stripMargin
    }
  }

  test("Adds @validateNewtype on string alias with constraint") {
    assertPresent("smithy4s.transformer.test#ValidatedString") {
      """|$version: "2.0"
         |
         |metadata smithy4sRenderValidatedNewtypes = true
         |
         |namespace smithy4s.transformer.test
         |
         |@length(min: 1, max: 10)
         |string ValidatedString
         |""".stripMargin
    }
  }

  test("Adds @validateNewtype on number alias with constraint") {
    assertPresent("smithy4s.transformer.test#ValidatedNumber") {
      """|$version: "2.0"
         |
         |metadata smithy4sRenderValidatedNewtypes = true
         |
         |namespace smithy4s.transformer.test
         |
         |@range(min: 1, max: 10)
         |integer ValidatedNumber
         |""".stripMargin
    }
  }

  test("Does not add @validateNewtype on unwrapped string alias") {
    assertMissing("smithy4s.transformer.test#ValidatedString") {
      """|$version: "2.0"
         |
         |metadata smithy4sRenderValidatedNewtypes = true
         |
         |namespace smithy4s.transformer.test
         |
         |use smithy4s.meta#unwrap
         |
         |@length(min: 1, max: 10)
         |@unwrap
         |string ValidatedString
         |""".stripMargin
    }
  }

  test(
    "Does not add @validateNewtype on type when smithy4sRenderValidatedNewtypes=false"
  ) {
    assertMissing("smithy4s.transformer.test#ValidatedString") {
      """|$version: "2.0"
         |
         |metadata smithy4sRenderValidatedNewtypes = false
         |
         |namespace smithy4s.transformer.test
         |
         |@length(min: 1, max: 10)
         |string ValidatedString
         |""".stripMargin
    }
  }

  test(
    "Does not add @validateNewtype on previously generated shapes with validatedNewtypes=false"
  ) {
    assertMissing("smithy4s.transformer.test#ValidatedString") {
      """|$version: "2.0"
         |
         |metadata smithy4sRenderValidatedNewtypes = true
         |metadata smithy4sGenerated = [{smithy4sVersion: "dev-SNAPSHOT", namespaces: ["smithy4s.transformer.test"], validatedNewtypes: false}]
         |
         |namespace smithy4s.transformer.test
         |
         |@length(min: 1, max: 10)
         |string ValidatedString
         |""".stripMargin
    }
  }

  test(
    "Adds @validateNewtype on previously generated shapes with validatedNewtypes=true"
  ) {
    assertPresent("smithy4s.transformer.test#ValidatedString") {
      """|$version: "2.0"
         |
         |metadata smithy4sRenderValidatedNewtypes = true
         |metadata smithy4sGenerated = [{smithy4sVersion: "dev-SNAPSHOT", namespaces: ["smithy4s.transformer.test"], validatedNewtypes: true}]
         |
         |namespace smithy4s.transformer.test
         |
         |@length(min: 1, max: 10)
         |string ValidatedString
         |""".stripMargin
    }
  }

  private def assertPresent(shapeId: String)(inputModel: String*)(implicit
      loc: munit.Location
  ): Unit = {
    val containsTrait = loadAndTransformModel(inputModel: _*)
      .expectShape(ShapeId.from(shapeId))
      .hasTrait(classOf[ValidateNewtypeTrait])

    assert(
      containsTrait,
      "Expected validateNewtype trait to be present"
    )
  }

  private def assertMissing(shapeId: String)(inputModel: String*)(implicit
      loc: munit.Location
  ): Unit = {
    val containsTrait = loadAndTransformModel(inputModel: _*)
      .expectShape(ShapeId.from(shapeId))
      .hasTrait(classOf[ValidateNewtypeTrait])

    assert(
      !containsTrait,
      "Expected validateNewtype trait to be missing"
    )
  }

  def loadAndTransformModel(inputModel: String*): Model =
    new ValidatedNewtypesTransformer()
      .transform(
        TransformContext
          .builder()
          .model(loadModel(inputModel: _*))
          .build()
      )

}
