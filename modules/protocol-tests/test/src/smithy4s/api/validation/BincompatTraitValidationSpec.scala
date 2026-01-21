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

import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.validation.ValidationEvent
import weaver._

import ModelUtils._

object BincompatTraitValidationSpec extends FunSuite {

  test("bincompatFriendly is allowed on structs") {
    assembleModel(
      s"""$$version: "2"
         |namespace test
         |use smithy4s.meta#bincompatFriendly
         |
         |@bincompatFriendly
         |structure SampleStruct {}
         |""".stripMargin
    ).unwrap()

    success
  }

  test("bincompatFriendly is OK on mixins") {
    assembleModel(
      s"""$$version: "2"
         |namespace test
         |use smithy4s.meta#bincompatFriendly
         |
         |@bincompatFriendly
         |@mixin
         |structure SampleStruct {}
         |""".stripMargin
    ).unwrap()

    success
  }

  test("bincompatFriendly is not allowed on errors") {
    // limitation: the `show` method of smithy4s throwables currently uses Product methods, and we don't implement Product

    val events = eventsWithoutLocations(
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |
           |@bincompatFriendly
           |@error("client")
           |structure SampleError {}
           |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("bincompatFriendly.NoErrors")
      .severity(Severity.ERROR)
      .message(
        "Found an incompatible shape when validating the constraints of the `smithy4s.meta#bincompatFriendly` trait attached to `test#SampleError`: A @bincompatFriendly structure must not have the error trait."
      )
      .shapeId(ShapeId.from("test#SampleError"))
      .build()

    expect(events.contains(expected)) || failure(events.toString())
  }

  test(
    "bincompatFriendly is not allowed on shapes used as operation inputs/outputs"
  ) {
    val events = eventsWithoutLocations(
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |
           |@bincompatFriendly
           |structure SampleInput {}
           |
           |operation SampleOperation {
           |  input: SampleInput
           |  output := @bincompatFriendly {}
           |}
           |""".stripMargin
      )
    )

    def errorEvent(forShape: ShapeId) =
      ValidationEvent
        .builder()
        .id("bincompatFriendly.NoInputOutput")
        .severity(Severity.ERROR)
        .message(
          s"Found an incompatible shape when validating the constraints of the `smithy4s.meta#bincompatFriendly` trait attached to `${forShape}`: A @bincompatFriendly structure must not be used as an operation input/output."
        )
        .shapeId(forShape)
        .build()

    val expectedForInput =
      errorEvent(ShapeId.from("test#SampleInput"))

    val expectedForOutput =
      errorEvent(ShapeId.from("test#SampleOperationOutput"))

    {
      expect(events.contains(expectedForInput)) &&
      expect(events.contains(expectedForOutput))
    } || failure(events.toString())
  }

  test(
    "sanity check: normal operations are valid, even if bincompatFriendly is used in the model"
  ) {
    assembleModel(
      s"""$$version: "2"
         |namespace test
         |
         |use smithy4s.meta#bincompatFriendly
         |
         |operation SampleOperation {
         |  input := {}
         |}
         |
         |@bincompatFriendly
         |structure SampleStruct { }
         |""".stripMargin
    ).unwrap()

    success
  }

  test("bincompatFriendly is allowed on unions") {
    assembleModel(
      s"""$$version: "2"
         |namespace test
         |use smithy4s.meta#bincompatFriendly
         |
         |@bincompatFriendly
         |union SampleUnion {
         |  exampleTarget: Unit
         |}
         |""".stripMargin
    ).unwrap()

    success
  }

  test("bincompatFriendly is not allowed on adt unions") {
    val events = eventsWithoutLocations(
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |use smithy4s.meta#adt
           |
           |@bincompatFriendly
           |@adt
           |union SampleAdtUnion {
           |  exampleTarget: Unit
           |}
           |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("TraitConflict")
      .severity(Severity.ERROR)
      .message(
        "Found conflicting traits on union shape: `smithy4s.meta#bincompatFriendly` conflicts with `smithy4s.meta#adt`"
      )
      .shapeId(ShapeId.from("test#SampleAdtUnion"))
      .build()

    expect(events.contains(expected)) || failure(events.toString())
  }

  test("bincompatFriendly is not allowed on adtMember structs") {
    val events = eventsWithoutLocations(
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |use smithy4s.meta#adtMember
           |
           |@bincompatFriendly
           |@adtMember("test#MyUnion")
           |structure SampleAdtMemberStruct { }
           |
           |union MyUnion {
           |  unionMember: SampleAdtMemberStruct
           |}
           |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("TraitConflict")
      .severity(Severity.ERROR)
      .message(
        "Found conflicting traits on structure shape: `smithy4s.meta#bincompatFriendly` conflicts with `smithy4s.meta#adtMember`"
      )
      .shapeId(ShapeId.from("test#SampleAdtMemberStruct"))
      .build()

    expect(events.contains(expected)) || failure(events.toString())
  }

  test(
    "bincompatFriendly is not allowed on unions, if there's a member of such a union that has an adtMember trait"
  ) {
    val events = eventsWithoutLocations(
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |use smithy4s.meta#adtMember
           |
           |@bincompatFriendly
           |union SampleUnion {
           |  s: MyStruct
           |}
           |
           |@adtMember(SampleUnion)
           |structure MyStruct {}
           |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("bincompatFriendly.NoAdtMemberTargets")
      .severity(Severity.ERROR)
      .message(
        "Found an incompatible shape when validating the constraints of the `smithy4s.meta#bincompatFriendly` trait attached to `test#SampleUnion`: Members of an @bincompatFriendly union must not target shapes that have the adtMember trait."
      )
      .shapeId(ShapeId.from("test#SampleUnion$s"))
      .build()

    expect(events.contains(expected)) || failure(events.toString())
  }

  test(
    "bincompatFriendly is not allowed on shapes targeted by adt unions"
  ) {
    val events = eventsWithoutLocations(
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |use smithy4s.meta#adt
           |
           |@adt
           |union SampleUnion {
           |  unionMember: SampleStruct
           |}
           |
           |@bincompatFriendly
           |structure SampleStruct {}
           |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("bincompatFriendly.NoAdtTargets")
      .severity(Severity.ERROR)
      .message(
        "Found an incompatible shape when validating the constraints of the `smithy4s.meta#bincompatFriendly` trait attached to `test#SampleStruct`: Shapes with the @bincompatFriendly trait must not be used as members of an adt union."
      )
      .shapeId(ShapeId.from("test#SampleUnion$unionMember"))
      .build()

    expect(events.contains(expected)) || failure(events.toString())
  }

  test("bincompatFriendly is allowed on traits") {
    assembleModel(
      s"""$$version: "2"
         |namespace test
         |use smithy4s.meta#bincompatFriendly
         |use smithy4s.meta#bincompatAdded
         |
         |@bincompatFriendly
         |@trait
         |structure SampleStruct {
         |  @bincompatAdded(version: "1.0.0")
         |  addedField: String
         |}
         |""".stripMargin
    ).unwrap()
    success
  }

  test("bincompatAdded is allowed inside bincompatFriendly structs") {
    assembleModel(
      s"""$$version: "2"
         |namespace test
         |use smithy4s.meta#bincompatFriendly
         |use smithy4s.meta#bincompatAdded
         |
         |@bincompatFriendly
         |structure SampleStruct {
         |  @bincompatAdded(version: "1.0.0")
         |  addedField: String
         |}
         |""".stripMargin
    ).unwrap()
    success
  }

  test("bincompatAdded is not allowed outside bincompatFriendly") {
    val events = eventsWithoutLocations(
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatAdded
           |
           |structure SampleStruct {
           |  @bincompatAdded(version: "1.0.0")
           |  addedField: String
           |}
           |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("TraitTarget")
      .severity(Severity.ERROR)
      .message(
        "Trait `smithy4s.meta#bincompatAdded` cannot be applied to `test#SampleStruct$addedField`. This trait may only be applied to shapes that match the following selector: structure[trait|smithy4s.meta#bincompatFriendly] > member"
      )
      .shapeId(ShapeId.from("test#SampleStruct$addedField"))
      .build()

    expect(events.contains(expected)) || failure(events.toString())
  }

  test("bincompatAdded is not allowed in bincompatFriendly unions") {
    val events = eventsWithoutLocations(
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |use smithy4s.meta#bincompatAdded
           |
           |@bincompatFriendly
           |union SampleUnion {
           |  s: String
           |  @bincompatAdded(version: "1.0.0")
           |  addedMember: String
           |}
           |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("TraitTarget")
      .severity(Severity.ERROR)
      .message(
        "Trait `smithy4s.meta#bincompatAdded` cannot be applied to `test#SampleUnion$addedMember`. This trait may only be applied to shapes that match the following selector: structure[trait|smithy4s.meta#bincompatFriendly] > member"
      )
      .shapeId(ShapeId.from("test#SampleUnion$addedMember"))
      .build()

    expect(events.contains(expected)) || failure(events.toString())
  }

  test("bincompatAdded is allowed on a required field with a default") {
    (
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |use smithy4s.meta#bincompatAdded
           |
           |@bincompatFriendly
           |structure SampleStruct {
           |  @bincompatAdded(version: "1.0.0")
           |  @required
           |  addedMember: String = "default member value"
           |}
           |""".stripMargin
      ).unwrap()
    )

    success
  }

  test("bincompatAdded is not allowed on a required field without a default") {
    val events = eventsWithoutLocations(
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |use smithy4s.meta#bincompatAdded
           |
           |@bincompatFriendly
           |structure SampleStruct {
           |  @bincompatAdded(version: "1.0.0")
           |  @required
           |  addedMember: String
           |}
           |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("bincompatAdded.MustHaveDefault")
      .severity(Severity.ERROR)
      .message(
        "Found an incompatible shape when validating the constraints of the `smithy4s.meta#bincompatAdded` trait attached to `test#SampleStruct$addedMember`: A @bincompatAdded required member must have a default value."
      )
      .shapeId(ShapeId.from("test#SampleStruct$addedMember"))
      .build()

    expect(events.contains(expected)) || failure(events.toString())
  }

  test("bincompatFriendly is allowed on enums") {
    assembleModel(
      s"""$$version: "2"
         |namespace test
         |use smithy4s.meta#bincompatFriendly
         |
         |@bincompatFriendly
         |enum SampleUnion {
         |  A
         |  B
         |  C
         |}
         |""".stripMargin
    ).unwrap()

    success
  }

  test("bincompatFriendly is allowed on intEnums") {
    assembleModel(
      s"""$$version: "2"
         |namespace test
         |use smithy4s.meta#bincompatFriendly
         |
         |@bincompatFriendly
         |intEnum SampleUnion {
         |  A = 1
         |  B = 2
         |  C = 3
         |}
         |""".stripMargin
    ).unwrap()

    success
  }

  validVersionFormatTest("1")
  validVersionFormatTest("1.0")
  validVersionFormatTest("0.1")
  validVersionFormatTest("1.2.3")
  validVersionFormatTest("1.2.3.4")

  invalidVersionFormatTest("")
  invalidVersionFormatTest("1.")
  invalidVersionFormatTest("1.a")
  invalidVersionFormatTest(".1")

  private def validVersionFormatTest(version: String) =
    test(
      s"bincompatAdded: accept valid version formats ($version)"
    ) {
      assembleModel(
        s"""$$version: "2"
           |namespace test
           |use smithy4s.meta#bincompatFriendly
           |use smithy4s.meta#bincompatAdded
           |
           |@bincompatFriendly
           |structure SampleStruct {
           |  @bincompatAdded(version: "$version")
           |  addedField: String
           |}
           |""".stripMargin
      ).unwrap()

      success
    }

  private def invalidVersionFormatTest(version: String) =
    test(
      s"bincompatAdded: reject invalid version formats ($version)"
    ) {
      val events = eventsWithoutLocations(
        assembleModel(s"""$$version: "2"
                         |namespace test
                         |use smithy4s.meta#bincompatFriendly
                         |use smithy4s.meta#bincompatAdded
                         |
                         |@bincompatFriendly
                         |structure SampleStruct {
                         |  @bincompatAdded(version: "$version")
                         |  addedField: String
                         |}
                         |""".stripMargin)
      )

      val expected = ValidationEvent
        .builder()
        .id("TraitValue")
        .severity(Severity.ERROR)
        .message(
          raw"""Error validating trait `smithy4s.meta#bincompatAdded`.version: String value provided for `smithy4s.meta#bincompatAdded$$version` must match regular expression: ^(\d+\.)*\d+$$"""
        )
        .shapeId(ShapeId.from("test#SampleStruct$addedField"))
        .build()

      expect(events.contains(expected)) || failure(events.toString())
    }

}
