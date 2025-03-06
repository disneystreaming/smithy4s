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

package smithy4s.api.validation

import smithy4s.meta.AdtMemberTrait
import smithy4s.meta.validation.AdtMemberTraitValidator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.validation.ValidationEvent
import weaver._

import scala.jdk.CollectionConverters._

object AdtMemberTraitValidatorSpec extends FunSuite {
  private val validator = new AdtMemberTraitValidator()

  test("return no error when union targets the structure") {
    val unionShapeId = ShapeId.fromParts("test", "MyUnion")
    val adtTrait = new AdtMemberTrait(unionShapeId)
    val structMember = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#String")
      .build()
    val struct =
      StructureShape
        .builder()
        .id("test#struct")
        .addTrait(adtTrait)
        .addMember(structMember)
        .build()

    val unionMember = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMember"))
      .target(struct.getId)
      .build()
    val union =
      UnionShape.builder().id(unionShapeId).addMember(unionMember).build()
    val model =
      Model.builder().addShapes(struct, union).build()

    val result = validator.validate(model).asScala.toList

    val expected = List.empty
    expect(result == expected)
  }

  test("return error when union does not target the structure") {
    val unionShapeId = ShapeId.fromParts("test", "MyUnion")
    val adtTrait = new AdtMemberTrait(unionShapeId)
    val structMember = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#String")
      .build()
    val struct =
      StructureShape
        .builder()
        .id("test#struct")
        .addTrait(adtTrait)
        .addMember(structMember)
        .build()

    val unionMember = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMember"))
      .target(ShapeId.fromParts("smithy.api", "String"))
      .build()
    val union =
      UnionShape.builder().id(unionShapeId).addMember(unionMember).build()

    val model =
      Model.builder().addShapes(struct, union).build()

    val result = validator.validate(model).asScala.toList

    val expected = List(
      ValidationEvent
        .builder()
        .id("AdtMemberTrait")
        .shape(struct)
        .severity(Severity.ERROR)
        .message(
          "This shape must be referenced by test#MyUnion because of its smithy4s.meta#adtMember trait"
        )
        .build()
    )
    expect(result == expected)
  }

  test("return no error when there are duplicate non-adtMember members") {
    val unionShapeId = ShapeId.fromParts("test", "MyUnion")
    val adtTrait = new AdtMemberTrait(unionShapeId)
    val structMember = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#String")
      .build()

    val struct =
      StructureShape
        .builder()
        .id("test#struct")
        .addTrait(adtTrait)
        .addMember(structMember)
        .build()

    val unionMember = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMember"))
      .target(struct.getId)
      .build()

    val unionMemberString1 = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMemberString1"))
      .target("smithy.api#String")
      .build()

    val unionMemberString2 = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMemberString2"))
      .target("smithy.api#String")
      .build()

    val union =
      UnionShape
        .builder()
        .id(unionShapeId)
        .addMember(unionMember)
        .addMember(unionMemberString1)
        .addMember(unionMemberString2)
        .build()

    val model =
      Model.builder().addShapes(struct, union).build()

    val result = validator.validate(model).asScala.toList

    val expected = List.empty
    expect(result == expected)
  }

  test("return error when structure is targeted by a union twice") {
    val unionShapeId = ShapeId.fromParts("test", "MyUnion")
    val adtTrait = new AdtMemberTrait(unionShapeId)
    val structMember = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#String")
      .build()
    val struct =
      StructureShape
        .builder()
        .id("test#struct")
        .addTrait(adtTrait)
        .addMember(structMember)
        .build()

    val unionMember = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMember"))
      .target(struct.getId)
      .build()

    val unionMember2 = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMember2"))
      .target(struct.getId)
      .build()

    val union =
      UnionShape
        .builder()
        .id(unionShapeId)
        .addMember(unionMember)
        .addMember(unionMember2)
        .build()

    val model =
      Model.builder().addShapes(struct, union).build()

    val result = validator.validate(model).asScala.toList

    val expected = List(
      ValidationEvent
        .builder()
        .id("AdtMemberTrait")
        .shape(unionMember)
        .severity(Severity.ERROR)
        .message(
          "Duplicate reference to shape test#struct in container test#MyUnion - only one is allowed"
        )
        .build()
    )
    expect(result == expected)
  }

  test("return error when structure is targeted by the wrong union") {
    val unionShapeId = ShapeId.fromParts("test", "MyUnion")
    val adtTrait = new AdtMemberTrait(unionShapeId)
    val stringShape = StringShape.builder().id("smithy.api#String").build()
    val structMember = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("test#String")
      .build()

    val struct =
      StructureShape
        .builder()
        .id("test#struct")
        .addTrait(adtTrait)
        .addMember(structMember)
        .build()

    val unionMember = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMember"))
      .target(stringShape.getId)
      .build()

    val union =
      UnionShape.builder().id(unionShapeId).addMember(unionMember).build()

    val union2ShapeId = ShapeId.fromParts("test", "MyUnionTwo")
    val union2Member = MemberShape
      .builder()
      .id(union2ShapeId.withMember("unionMember"))
      .target(struct.getId)
      .build()

    val union2 =
      UnionShape.builder().id(union2ShapeId).addMember(union2Member).build()

    val model =
      Model.builder().addShapes(struct, stringShape, union, union2).build()

    val result = validator.validate(model).asScala.toList

    val expected = List(
      ValidationEvent
        .builder()
        .id("AdtMemberTrait")
        .shape(struct)
        .severity(Severity.ERROR)
        .message(
          "This shape must be referenced by test#MyUnion because of its smithy4s.meta#adtMember trait"
        )
        .build(),
      ValidationEvent
        .builder()
        .id("AdtMemberTrait")
        .shape(union2Member)
        .severity(Severity.ERROR)
        .message(
          "Invalid reference to test#struct - due to its smithy4s.meta#adtMember trait, only test#MyUnion can reference it"
        )
        .build()
    )
    expect(result == expected)
  }

  test("return error when structure is targeted by multiple unions") {
    val unionShapeId = ShapeId.fromParts("test", "MyUnion")
    val adtTrait = new AdtMemberTrait(unionShapeId)
    val structMember = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#String")
      .build()
    val struct =
      StructureShape
        .builder()
        .id("test#struct")
        .addTrait(adtTrait)
        .addMember(structMember)
        .build()

    val unionMember = MemberShape
      .builder()
      .id(unionShapeId.withMember("unionMember"))
      .target(struct.getId)
      .build()
    val union =
      UnionShape.builder().id(unionShapeId).addMember(unionMember).build()

    val union2ShapeId = ShapeId.fromParts("test", "MyUnionTwo")
    val unionMember2 = unionMember.toBuilder
      .id(union2ShapeId.withMember("unionMemberTwo"))
      .build()

    val union2 =
      UnionShape.builder().id(union2ShapeId).addMember(unionMember2).build()

    val model =
      Model.builder().addShapes(struct, union, union2).build()

    val result = validator.validate(model).asScala.toList

    val expected = List(
      ValidationEvent
        .builder()
        .id("AdtMemberTrait")
        .shape(unionMember2)
        .severity(Severity.ERROR)
        .message(
          "Invalid reference to test#struct - due to its smithy4s.meta#adtMember trait, only test#MyUnion can reference it"
        )
        .build()
    )
    expect(result == expected)
  }
}
