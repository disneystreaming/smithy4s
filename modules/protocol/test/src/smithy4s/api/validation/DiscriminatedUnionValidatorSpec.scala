/*
 *  Copyright 2021 Disney Streaming
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

import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.model.shapes.MemberShape
import smithy4s.api.DiscriminatedUnionTrait
import software.amazon.smithy.model.Model
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.validation.ValidationEvent
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.ShapeId

object DiscriminatedUnionValidatorSpec extends weaver.FunSuite {

  private val validator = new DiscriminatedUnionValidator()

  test("catch unions with non-structure members") {
    val member =
      MemberShape
        .builder()
        .target("smithy.api#String")
        .id("test#test$test")
        .build()
    val union = UnionShape
      .builder()
      .addTrait(new DiscriminatedUnionTrait("type"))
      .id("test#test")
      .addMember(member)
      .build()

    val model =
      Model.builder().addShape(union).build()

    val result = validator.validate(model).asScala.toList

    val expected = List(
      ValidationEvent
        .builder()
        .id("DiscriminatedUnion")
        .shape(member)
        .severity(Severity.ERROR)
        .message(
          "Target of member 'test' is not a structure shape"
        )
        .build()
    )
    expect(result == expected)
  }

  test("return error when target structures contain discriminator") {
    val struct = StructureShape
      .builder()
      .id("test#struct")
      .addMember("type", ShapeId.fromParts("smithy.api", "String"))
      .build()
    val member =
      MemberShape
        .builder()
        .target("test#struct")
        .id("test#test$test")
        .build()
    val union = UnionShape
      .builder()
      .addTrait(new DiscriminatedUnionTrait("type"))
      .id("test#test")
      .addMember(member)
      .build()

    val model =
      Model.builder().addShapes(struct, union).build()

    val result = validator.validate(model).asScala.toList

    val expected = List(
      ValidationEvent
        .builder()
        .id("DiscriminatedUnion")
        .shape(member)
        .severity(Severity.ERROR)
        .message(
          "Target of member 'test' contains discriminator 'type'"
        )
        .build()
    )
    expect(result == expected)
  }

  test("return no error when union contains only structure members") {
    val struct = StructureShape.builder().id("test#struct").build()
    val member =
      MemberShape
        .builder()
        .target("test#struct")
        .id("test#test$test")
        .build()
    val union = UnionShape
      .builder()
      .addTrait(new DiscriminatedUnionTrait("type"))
      .id("test#test")
      .addMember(member)
      .build()

    val model =
      Model.builder().addShapes(struct, union).build()

    val result = validator.validate(model).asScala.toList

    expect(result == List.empty)
  }

}
