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
import smithy4s.meta.validation.RefinementTraitValidator
import software.amazon.smithy.model.SourceLocation

object RefinementTraitValidatorSpec extends weaver.FunSuite {

  private def validator = new RefinementTraitValidator()

  test(
    "validation events are returned when multiple refinements are applied to one shape"
  ) {
    val model = loadModel(
      """|namespace test
         |
         |use smithy4s.meta#refinement
         |
         |@trait()
         |@refinement(targetType: "test.one", providerImport: "test.one.prov")
         |structure trtOne {}
         |
         |@trait()
         |@refinement(targetType: "test.two", providerImport: "test.two.prov")
         |structure trtTwo {}
         |
         |@trtOne
         |@trtTwo
         |integer TestIt
         |""".stripMargin
    )

    val result = validator.validate(model).asScala.toList
    val expected = List(
      ValidationEvent
        .builder()
        .sourceLocation(new SourceLocation("test.smithy", 15, 1))
        .id("RefinementTrait")
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
    val model = loadModel(
      """|namespace test
         |
         |use smithy4s.meta#refinement
         |
         |@trait()
         |@refinement(targetType: "test.one", providerImport: "test.one.prov")
         |structure trtOne {}
         |
         |@trait()
         |@refinement(targetType: "test.two", providerImport: "test.two.prov")
         |structure trtTwo {}
         |
         |@trtOne
         |integer TestIt
         |""".stripMargin
    )

    val result = validator.validate(model).asScala.toList
    val expected = List.empty
    expect.same(result, expected)
  }

  test(
    "no validation events are returned when all shapes have only one refinement"
  ) {
    val model = loadModel(
      """|namespace test
         |
         |use smithy4s.meta#refinement
         |
         |@trait()
         |@refinement(targetType: "test.one", providerImport: "test.one.prov")
         |structure trtOne {}
         |
         |@trait()
         |@refinement(targetType: "test.two", providerImport: "test.two.prov")
         |structure trtTwo {}
         |
         |@trtOne
         |integer TestIt
         |
         |@trtTwo
         |integer TestItAgain
         |""".stripMargin
    )

    val result = validator.validate(model).asScala.toList
    val expected = List.empty
    expect.same(result, expected)
  }

  test(
    "validation events are returned when using refinement trait on disallowed shape"
  ) {
    val model = loadModel(
      """|namespace test
         |
         |use smithy4s.meta#refinement
         |
         |@trait()
         |@refinement(targetType: "test.one", providerImport: "test.one.prov")
         |structure trtOne {}
         |
         |@trtOne
         |structure TestIt {}
         |""".stripMargin
    )

    val result = validator.validate(model).asScala.toList
    val expected = List(
      ValidationEvent
        .builder()
        .sourceLocation(new SourceLocation("test.smithy", 10, 1))
        .id("RefinementTrait")
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
    val model = loadModel(
      """|namespace test
         |
         |use smithy4s.meta#refinement
         |
         |@trait()
         |@refinement(targetType: "test.one", providerImport: "test.one.prov")
         |structure trtOne {}
         |
         |@trtOne
         |@enum([
         |  { value: "A" },
         |  { value: "B" }
         |])
         |string TestIt
         |""".stripMargin
    )

    val result = validator.validate(model).asScala.toList
    val expected = List(
      ValidationEvent
        .builder()
        .sourceLocation(new SourceLocation("test.smithy", 14, 1))
        .id("RefinementTrait")
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
    "check that provider import format is valid"
  ) {

    def mkModelString(targetType: String, providerImport: String) =
      s"""|$$version: "2.0"
          |
          |namespace test
          |
          |use smithy4s.meta#refinement
          |
          |@trait()
          |@refinement(targetType: "$targetType", providerImport: "$providerImport")
          |structure trtOne {}
          |
          |@trtOne
          |string TestItOne
          |""".stripMargin

    def runTest(targetType: String, providerImport: String) = {
      val modelString = mkModelString(targetType, providerImport)
      val result = Model
        .assembler()
        .discoverModels()
        .addUnparsedModel("test.smithy", modelString)
        .assemble()
        .getValidationEvents()
        .asScala
        .toList

      expect.same(result, List.empty)
    }

    runTest("_root_.test.one", "_root_.test._") &&
    runTest("test.one", "test._") &&
    runTest("_root_.test.one", "_root_.test.given") &&
    runTest("test.one", "test.given")
  }

  test(
    "check that provider import format is valid - should fail"
  ) {
    val result = loadValidationErrors(
      """|$version: "2.0"
         |
         |namespace test
         |
         |use smithy4s.meta#refinement
         |
         |@trait()
         |@refinement(targetType: "test.one", providerImport: "(&$^#%@$!")
         |structure trtOne {}
         |
         |@trtOne
         |string TestIt
         |""".stripMargin
    )

    val expected = List(
      ValidationEvent
        .builder()
        .id("TraitValue")
        .shapeId(ShapeId.fromParts("test", "trtOne"))
        .severity(Severity.ERROR)
        .message(
          "Error validating trait `smithy4s.meta#refinement`.providerImport: String value provided for `smithy4s.meta#Import` must match regular expression: ^(?:_root_\\.)?(?:[a-zA-Z`][\\w]*\\.?)*\\.(?:_|given)$"
        )
        .build()
    )

    expect.same(result, expected)
  }

  test(
    "no validation events are returned when one refinement is applied to a simple member shape"
  ) {
    val model = loadModel(
      """|namespace test
         |
         |use smithy4s.meta#refinement
         |
         |@trait()
         |@refinement(targetType: "test.one", providerImport: "test.one.prov")
         |structure trtOne {}
         |
         |structure SomeTest{
         |  @trtOne
         |  value: String
         |}
         |""".stripMargin
    )

    val result = validator.validate(model).asScala.toList
    val expected = List.empty
    expect.same(result, expected)
  }

  private def loadModel(modelString: String): Model = {
    Model
      .assembler()
      .disableValidation()
      .discoverModels()
      .addUnparsedModel("test.smithy", modelString)
      .assemble()
      .unwrap()
  }

  private def loadValidationErrors(
      modelString: String
  ): List[ValidationEvent] = {
    Model
      .assembler()
      .discoverModels()
      .addUnparsedModel("test.smithy", modelString)
      .assemble()
      .getValidationEvents()
      .asScala
      .toList
  }

}
