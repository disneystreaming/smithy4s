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

package smithy4s.api.validation

import weaver._
import smithy4s.meta.ErrorMessageTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._

import scala.jdk.CollectionConverters._
import smithy4s.meta.validation.ErrorMessageTraitValidator
import software.amazon.smithy.model.validation.ValidationEvent
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.traits.ErrorTrait

object ErrorMessageTraitValidatorSpec extends FunSuite {
  private val validator = new ErrorMessageTraitValidator()
  private val smithy4sMetaImport = classOf[ErrorMessageTrait]
    .getClassLoader()
    .getResource("META-INF/smithy/smithy4s.meta.smithy")
  private def noErrorTrait(s: Shape) = ValidationEvent
    .builder()
    .id("ErrorMessageTrait")
    .shape(s)
    .severity(Severity.ERROR)
    .message(
      "the structure containing the member annotated with @errorMessage has to be annotated with @error"
    )
    .build()

  test("return no error annotation is correctly used") {
    val theTrait = new ErrorMessageTrait()
    val structMember = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#String")
      .addTrait(theTrait)
      .build()
    val struct =
      StructureShape
        .builder()
        .id("test#struct")
        .addTrait(new ErrorTrait("client"))
        .addMember(structMember)
        .build()

    val model =
      Model
        .assembler()
        .addShape(struct)
        .addImport(smithy4sMetaImport)
        .assemble()
        .unwrap()

    val result = validator.validate(model).asScala.toList

    val expected = List.empty
    expect(result == expected)
  }

  test("fail when used on a member that's not a string") {
    val theTrait = new ErrorMessageTrait()
    val structMember = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#Integer")
      .addTrait(theTrait)
      .build()
    val struct =
      StructureShape
        .builder()
        .id("test#struct")
        .addTrait(new ErrorTrait("client"))
        .addMember(structMember)
        .build()

    val model =
      Model
        .assembler()
        .addShape(struct)
        .addImport(smithy4sMetaImport)
        .assemble()

    val expected = List(
      ValidationEvent
        .builder()
        .id("ErrorMessageTrait")
        .shape(structMember)
        .severity(Severity.ERROR)
        .message(
          "@errorMessage should only be used on member that target a String shape"
        )
        .build()
    )
    expect(model.getValidationEvents().asScala.toList == expected)
  }

  test("fails when parent structure has no `error` trait") {
    val theTrait = new ErrorMessageTrait()
    val structMember = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#String")
      .addTrait(theTrait)
      .build()
    val struct =
      StructureShape
        .builder()
        .id("test#struct")
        .addMember(structMember)
        .build()

    val model =
      Model
        .assembler()
        .addShape(struct)
        .addImport(smithy4sMetaImport)
        .assemble()

    val expected = List(noErrorTrait(struct))
    expect(model.getValidationEvents().asScala.toList == expected)
  }

  test("fails when used on non struct member") {
    val theTrait = new ErrorMessageTrait()
    val unionShapeId = ShapeId.fromParts("test", "MyUnion")
    val unionMember = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMember"))
      .addTrait(theTrait)
      .target("smithy.api#String")
      .build()
    val union =
      UnionShape
        .builder()
        .id(unionShapeId)
        .addMember(unionMember)
        .build()

    val model =
      Model
        .assembler()
        .addShape(union)
        .addImport(smithy4sMetaImport)
        .assemble()

    val expected = List(
      noErrorTrait(union),
      ValidationEvent
        .builder()
        .id("TraitTarget")
        .shape(unionMember)
        .severity(Severity.ERROR)
        .message(
          s"Trait `smithy4s.meta#errorMessage` cannot be applied to `test#MyUnion$$unionMember`. This trait may only be applied to shapes that match the following selector: structure > member"
        )
        .build()
    )
    expect(model.getValidationEvents().asScala.toList == expected)
  }
}
