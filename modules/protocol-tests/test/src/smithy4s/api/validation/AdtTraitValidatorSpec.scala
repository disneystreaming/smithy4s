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

import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.validation.ValidationEvent
import weaver._

import ModelUtils._

object AdtTraitValidatorSpec extends FunSuite {

  test("AdtTrait - not allowed on mixins") {

    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#adt
          |
          |structure struct {
          |  testing: String
          |}
          |
          |@adt
          |@mixin
          |union MyUnion {
          |  unionMember: struct
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("TraitTarget")
      .shapeId(ShapeId.fromParts("test", "MyUnion"))
      .severity(Severity.ERROR)
      .message(
        "Trait `smithy4s.meta#adt` cannot be applied to `test#MyUnion`. This trait may only be applied to shapes that match the following selector: union :not([trait|mixin])"
      )
      .build()

    assert(events.contains(expected))
  }

  test("AdtTrait - return no error when union targets the structure") {
    assembleModel(
      """$version: "2"
        |namespace test
        |use smithy4s.meta#adt
        |
        |structure struct {
        |  testing: String
        |}
        |
        |@adt
        |union MyUnion {
        |  unionMember: struct
        |}
        |""".stripMargin
    ).unwrap()

    success
  }

  test("AdtTrait - return error when the union targets a non-structure") {
    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |use smithy4s.meta#adt
          |
          |@adt
          |union MyUnion {
          |  unionMember: String
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("AdtTrait")
      .shapeId(ShapeId.fromParts("test", "MyUnion"))
      .severity(Severity.ERROR)
      .message(
        "All members of an adt union must be structures"
      )
      .build()

    assert(events.contains(expected))
  }

  test(
    "AdtTrait - return error when the union targets a structure AND a non-stru ture"
  ) {
    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |use smithy4s.meta#adt
          |
          |structure struct {
          |  testing: String
          |}
          |
          |@adt
          |union MyUnion {
          |  unionMember: String
          |  anotherMEmber: struct
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("AdtTrait")
      .shapeId(ShapeId.fromParts("test", "MyUnion"))
      .severity(Severity.ERROR)
      .message(
        "All members of an adt union must be structures"
      )
      .build()

    assert(events.contains(expected))
  }

  test(
    "AdtTrait - return error when structure is targeted by multiple unions"
  ) {

    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#adt
          |
          |structure struct {
          |  testing: String
          |}
          |
          |@adt
          |union MyUnion {
          |  unionMember: struct
          |}
          |
          |@adt
          |union MyUnionTwo {
          |  unionMemberTwo: struct
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("AdtTrait")
      .shapeId(ShapeId.fromParts("test", "struct"))
      .severity(Severity.ERROR)
      .message(
        "This shape can only be referenced once and from one adt union, but it's referenced from test#MyUnion, test#MyUnionTwo"
      )
      .build()

    assert(events.contains(expected))
  }

  test(
    "AdtTrait - return error when structure is targeted by the same union twice"
  ) {

    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#adt
          |
          |structure struct {
          |  testing: String
          |}
          |
          |@adt
          |union MyUnion {
          |  unionMember: struct
          |  unionMember2: struct
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("AdtTrait")
      .shapeId(ShapeId.fromParts("test", "struct"))
      .severity(Severity.ERROR)
      .message(
        "This shape can only be referenced once and from one adt union, but it's referenced from test#MyUnion (2 times)"
      )
      .build()

    assert(events.contains(expected))
  }

  test(
    "AdtTrait - return error when structure is targeted by a union and a structure"
  ) {
    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#adt
          |
          |structure struct {
          |  testing: String
          |}
          |
          |@adt
          |union MyUnion {
          |  unionMember: struct
          |}
          |
          |structure MyStruct2 {
          |  structMember2: struct
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("AdtTrait")
      .shapeId(ShapeId.fromParts("test", "struct"))
      .severity(Severity.ERROR)
      .message(
        "This shape can only be referenced once and from one adt union, but it's referenced from test#MyStruct2, test#MyUnion"
      )
      .build()

    assert(events.contains(expected))
  }
}
