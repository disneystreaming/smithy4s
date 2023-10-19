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
import smithy4s.meta.AdtMemberTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.validation.{Severity, ValidationEvent}

import scala.jdk.CollectionConverters._
import smithy4s.meta.validation.AdtMemberTraitValidator

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
        .id("AdtValidator")
        .shape(struct)
        .severity(Severity.ERROR)
        .message(
          "test#MyUnion must have exactly one member targeting test#struct"
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
        .id("AdtValidator")
        .shape(union2)
        .severity(Severity.ERROR)
        .message(
          "ADT member test#struct must not be referenced in any other shape but test#MyUnion"
        )
        .build()
    )
    expect(result == expected)
  }

  test("return error when structure is targeted by a union and a structure") {
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

    val struct2ShapeId = ShapeId.fromParts("test", "MyStruct2")
    val structMember2 = unionMember.toBuilder
      .id(struct2ShapeId.withMember("structMember2"))
      .build()
    val struct2 = StructureShape
      .builder()
      .id(struct2ShapeId)
      .addMember(structMember2)
      .build()

    val model =
      Model.builder().addShapes(struct, union, struct2).build()

    val result = validator.validate(model).asScala.toList

    val expected = List(
      ValidationEvent
        .builder()
        .id("AdtValidator")
        .shape(struct2)
        .severity(Severity.ERROR)
        .message(
          "ADT member test#struct must not be referenced in any other shape but test#MyUnion"
        )
        .build()
    )
    expect(result == expected)
  }
}
