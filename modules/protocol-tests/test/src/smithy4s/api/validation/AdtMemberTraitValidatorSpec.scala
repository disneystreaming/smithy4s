/*
 *  Copyright 2021-2026 Disney Streaming
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

object AdtMemberTraitValidatorSpec extends FunSuite {

  test("return no error when union targets the structure") {

    assembleModel(
      """$version: "2"
        |namespace test
        |
        |use smithy4s.meta#adtMember
        |
        |@adtMember("test#MyUnion")
        |structure struct {
        |  testing: String
        |}
        |
        |union MyUnion {
        |  unionMember: struct
        |}
        |""".stripMargin
    ).unwrap()

    success
  }

  test("return an error when the union is a mixin") {

    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#adtMember
          |
          |@adtMember("test#MyUnion")
          |structure struct {
          |  testing: String
          |}
          |
          |@mixin
          |union MyUnion {
          |  unionMember: struct
          |}
          |""".stripMargin
      )
    )

    val expected =
      ValidationEvent
        .builder()
        .id("TraitValue")
        .shapeId(ShapeId.fromParts("test", "struct"))
        .severity(Severity.ERROR)
        .message(
          "Error validating trait `smithy4s.meta#adtMember`: Shape ID `test#MyUnion` does not match selector `union :not([trait|mixin])`"
        )
        .build()

    expect(events.contains(expected))
  }

  test("return error when union does not target the structure") {
    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#adtMember
          |
          |@adtMember("test#MyUnion")
          |structure struct {
          |  testing: String
          |}
          |
          |union MyUnion {
          |  unionMember: String
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("AdtMemberTrait")
      .shapeId(ShapeId.fromParts("test", "struct"))
      .severity(Severity.ERROR)
      .message(
        "This shape must be referenced by test#MyUnion because of its smithy4s.meta#adtMember trait"
      )
      .build()

    expect(events.contains(expected))
  }

  test("return no error when there are duplicate non-adtMember members") {

    assembleModel(
      """$version: "2"
        |namespace test
        |
        |use smithy4s.meta#adtMember
        |
        |@adtMember("test#MyUnion")
        |structure struct {
        |  testing: String
        |}
        |
        |union MyUnion {
        |  unionMember: struct
        |  unionMemberString1: String
        |  unionMemberString2: String
        |}
        |""".stripMargin
    ).unwrap()

    success
  }

  test("return error when structure is targeted by a union twice") {
    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#adtMember
          |
          |@adtMember("test#MyUnion")
          |structure struct {
          |  testing: String
          |}
          |
          |union MyUnion {
          |  unionMember: struct
          |  unionMember2: struct
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("AdtMemberTrait")
      .shapeId(ShapeId.fromParts("test", "MyUnion", "unionMember"))
      .severity(Severity.ERROR)
      .message(
        "Duplicate reference to shape test#struct in container test#MyUnion - only one is allowed"
      )
      .build()

    expect(events.contains(expected))
  }

  test("return error when structure is targeted by the wrong union") {

    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#adtMember
          |
          |@adtMember("test#MyUnion")
          |structure struct {
          |  testing: String
          |}
          |
          |union MyUnion {
          |  unionMember: String
          |}
          |
          |union MyUnionTwo {
          |  unionMember: struct
          |}
          |""".stripMargin
      )
    )

    val expected = List(
      ValidationEvent
        .builder()
        .id("AdtMemberTrait")
        .shapeId(ShapeId.fromParts("test", "struct"))
        .severity(Severity.ERROR)
        .message(
          "This shape must be referenced by test#MyUnion because of its smithy4s.meta#adtMember trait"
        )
        .build(),
      ValidationEvent
        .builder()
        .id("AdtMemberTrait")
        .shapeId(ShapeId.fromParts("test", "MyUnionTwo", "unionMember"))
        .severity(Severity.ERROR)
        .message(
          "Invalid reference to test#struct - due to its smithy4s.meta#adtMember trait, only test#MyUnion can reference it"
        )
        .build()
    )

    forEach(expected) { e =>
      expect(events.contains(e))
    }
  }

  test("return error when structure is targeted by multiple unions") {

    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#adtMember
          |
          |@adtMember("test#MyUnion")
          |structure struct {
          |  testing: String
          |}
          |
          |union MyUnion {
          |  unionMember: struct
          |}
          |
          |union MyUnionTwo {
          |  unionMemberTwo: struct
          |}
          |""".stripMargin
      )
    )

    val expected =
      ValidationEvent
        .builder()
        .id("AdtMemberTrait")
        .shapeId(ShapeId.fromParts("test", "MyUnionTwo", "unionMemberTwo"))
        .severity(Severity.ERROR)
        .message(
          "Invalid reference to test#struct - due to its smithy4s.meta#adtMember trait, only test#MyUnion can reference it"
        )
        .build()

    expect(events.contains(expected))
  }

  test(
    "Using adt and adtMember together is allowed - all members marked with adtMember"
  ) {

    assembleModel(
      """$version: "2"
        |namespace test
        |
        |use smithy4s.meta#adt
        |use smithy4s.meta#adtMember
        |
        |@adtMember("test#MyUnion")
        |structure struct {
        |  testing: String
        |}
        |
        |@adtMember("test#MyUnion")
        |structure struct2 {
        |  testing: String
        |}
        |
        |@adt
        |union MyUnion {
        |  unionMember: struct
        |  unionMember2: struct2
        |}
        |""".stripMargin
    ).unwrap()

    success
  }
  test(
    "Using adt and adtMember together is allowed - some members tagged with adtMember"
  ) {

    assembleModel(
      """$version: "2"
        |namespace test
        |
        |use smithy4s.meta#adt
        |use smithy4s.meta#adtMember
        |
        |@adtMember("test#MyUnion")
        |structure struct {
        |  testing: String
        |}
        |
        |@adtMember("test#MyUnion")
        |structure struct2 {
        |  testing: String
        |}
        |
        |structure struct3 {}
        |
        |@adt
        |union MyUnion {
        |  unionMember: struct
        |  unionMember2: struct2
        |  unionMember3: struct3
        |}
        |""".stripMargin
    ).unwrap()

    success
  }
}
