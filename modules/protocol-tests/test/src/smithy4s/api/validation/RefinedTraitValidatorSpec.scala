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

package smithy4s.api.validation

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.validation._

import scala.jdk.CollectionConverters._
import smithy4s.meta.validation.RefinedTraitValidator
import software.amazon.smithy.model.SourceLocation

object RefinedTraitValidatorSpec extends weaver.FunSuite {

  private def validator = new RefinedTraitValidator()

  test(
    "validation events are returned when multiple refinements are applied to one shape"
  ) {
    val modelString = """|namespace test
                         |
                         |use smithy4s.meta#refined
                         |
                         |@trait()
                         |@refined(targetClasspath: "test.one", providerClasspath: "test.one.prov")
                         |structure trtOne {}
                         |
                         |@trait()
                         |@refined(targetClasspath: "test.two", providerClasspath: "test.two.prov")
                         |structure trtTwo {}
                         |
                         |@trtOne
                         |@trtTwo
                         |integer TestIt
                         |""".stripMargin

    val model = Model
      .assembler()
      .disableValidation()
      .discoverModels()
      .addUnparsedModel("test.smithy", modelString)
      .assemble()
      .unwrap()

    val result = validator.validate(model).asScala.toList
    val expected = List(
      ValidationEvent
        .builder()
        .sourceLocation(new SourceLocation("test.smithy", 15, 1))
        .id("RefinedTrait")
        .shapeId(ShapeId.fromParts("test", "TestIt"))
        .severity(Severity.ERROR)
        .message(
          "Shapes may only be annotated with one refinement trait"
        )
        .build()
    )
    expect.same(result, expected)
  }

  test(
    "no validation events are returned when one refinement is applied to a shape"
  ) {
    val modelString = """|namespace test
                         |
                         |use smithy4s.meta#refined
                         |
                         |@trait()
                         |@refined(targetClasspath: "test.one", providerClasspath: "test.one.prov")
                         |structure trtOne {}
                         |
                         |@trait()
                         |@refined(targetClasspath: "test.two", providerClasspath: "test.two.prov")
                         |structure trtTwo {}
                         |
                         |@trtOne
                         |integer TestIt
                         |""".stripMargin

    val model = Model
      .assembler()
      .disableValidation()
      .discoverModels()
      .addUnparsedModel("test.smithy", modelString)
      .assemble()
      .unwrap()

    val result = validator.validate(model).asScala.toList
    val expected = List.empty
    expect.same(result, expected)
  }

  test(
    "no validation events are returned when all shapes have only one refinement"
  ) {
    val modelString = """|namespace test
                         |
                         |use smithy4s.meta#refined
                         |
                         |@trait()
                         |@refined(targetClasspath: "test.one", providerClasspath: "test.one.prov")
                         |structure trtOne {}
                         |
                         |@trait()
                         |@refined(targetClasspath: "test.two", providerClasspath: "test.two.prov")
                         |structure trtTwo {}
                         |
                         |@trtOne
                         |integer TestIt
                         |
                         |@trtTwo
                         |integer TestItAgain
                         |""".stripMargin

    val model = Model
      .assembler()
      .disableValidation()
      .discoverModels()
      .addUnparsedModel("test.smithy", modelString)
      .assemble()
      .unwrap()

    val result = validator.validate(model).asScala.toList
    val expected = List.empty
    expect.same(result, expected)
  }

  test(
    "validation events are returned when using refinement trait on disallowed shape"
  ) {
    val modelString = """|namespace test
                         |
                         |use smithy4s.meta#refined
                         |
                         |@trait()
                         |@refined(targetClasspath: "test.one", providerClasspath: "test.one.prov")
                         |structure trtOne {}
                         |
                         |@trtOne
                         |structure TestIt {}
                         |""".stripMargin

    val model = Model
      .assembler()
      .disableValidation()
      .discoverModels()
      .addUnparsedModel("test.smithy", modelString)
      .assemble()
      .unwrap()

    val result = validator.validate(model).asScala.toList
    val expected = List(
      ValidationEvent
        .builder()
        .sourceLocation(new SourceLocation("test.smithy", 10, 1))
        .id("RefinedTrait")
        .shapeId(ShapeId.fromParts("test", "TestIt"))
        .severity(Severity.ERROR)
        .message(
          "refinements can only be used on simpleShapes, list, set, and map. Simple shapes must not be constrained by enum, length, range, or pattern traits"
        )
        .build()
    )
    expect.same(result, expected)
  }

  test(
    "validation events are returned when using refinement trait on disallowed shape - enum"
  ) {
    val modelString = """|namespace test
                         |
                         |use smithy4s.meta#refined
                         |
                         |@trait()
                         |@refined(targetClasspath: "test.one", providerClasspath: "test.one.prov")
                         |structure trtOne {}
                         |
                         |@trtOne
                         |@enum([
                         |  { value: "A" },
                         |  { value: "B" }
                         |])
                         |string TestIt
                         |""".stripMargin

    val model = Model
      .assembler()
      .disableValidation()
      .discoverModels()
      .addUnparsedModel("test.smithy", modelString)
      .assemble()
      .unwrap()

    val result = validator.validate(model).asScala.toList
    val expected = List(
      ValidationEvent
        .builder()
        .sourceLocation(new SourceLocation("test.smithy", 14, 1))
        .id("RefinedTrait")
        .shapeId(ShapeId.fromParts("test", "TestIt"))
        .severity(Severity.ERROR)
        .message(
          "refinements can only be used on simpleShapes, list, set, and map. Simple shapes must not be constrained by enum, length, range, or pattern traits"
        )
        .build()
    )
    expect.same(result, expected)
  }

}
