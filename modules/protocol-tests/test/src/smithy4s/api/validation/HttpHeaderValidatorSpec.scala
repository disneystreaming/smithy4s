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

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.{MemberShape, StructureShape}
import software.amazon.smithy.model.traits.HttpHeaderTrait
import software.amazon.smithy.model.validation.{Severity, ValidationEvent}

import scala.jdk.CollectionConverters._

object HttpHeaderValidatorSpec extends weaver.FunSuite {

  test("reject models with content-type header") {
    val validator = new HttpHeaderValidator()
    val member = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#String")
      .addTrait(new HttpHeaderTrait("Content-Type"))
      .build()
    val struct =
      StructureShape.builder().id("test#struct").addMember(member).build()

    val model =
      Model.builder().addShape(struct).build()

    val result = validator.validate(model).asScala.toList

    val expected = List(
      ValidationEvent
        .builder()
        .id("HttpHeader")
        .shape(member)
        .severity(Severity.WARNING)
        .message(
          "Header named `Content-Type` may be overridden in client/server implementations"
        )
        .build()
    )
    expect(result == expected)
  }

  test("accept random arbitrary header") {
    val validator = new HttpHeaderValidator()
    val member = MemberShape
      .builder()
      .id("test#struct$testing")
      .target("smithy.api#String")
      .addTrait(new HttpHeaderTrait("random"))
      .build()
    val struct =
      StructureShape.builder().id("test#struct").addMember(member).build()

    val model =
      Model.builder().addShape(struct).build()

    val result = validator.validate(model).asScala.toList

    expect(result == List.empty)
  }

}
