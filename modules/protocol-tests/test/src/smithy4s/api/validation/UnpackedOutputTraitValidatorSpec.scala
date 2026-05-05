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

object UnpackedOutputTraitValidatorSpec extends FunSuite {

  test(
    "UnpackedOutputTrait - no error when output structure has exactly one member"
  ) {
    assembleModel(
      """$version: "2"
        |namespace test
        |
        |use smithy4s.meta#unpackedOutput
        |
        |@unpackedOutput
        |operation MyOp {
        |  output := {
        |    item: String
        |  }
        |}
        |""".stripMargin
    ).unwrap()

    success
  }

  test(
    "UnpackedOutputTrait - error when output structure has no members"
  ) {
    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#unpackedOutput
          |
          |@unpackedOutput
          |operation MyOp {
          |  output := {}
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("UnpackedOutputTrait")
      .shapeId(ShapeId.fromParts("test", "MyOp"))
      .severity(Severity.ERROR)
      .message(
        "Operations annotated with @unpackedOutput must have an output structure with exactly one member, but test#MyOpOutput has 0"
      )
      .build()

    expect(events.contains(expected))
  }

  test(
    "UnpackedOutputTrait - error when output structure has more than one member"
  ) {
    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#unpackedOutput
          |
          |@unpackedOutput
          |operation MyOp {
          |  output := {
          |    item: String
          |    other: Integer
          |  }
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("UnpackedOutputTrait")
      .shapeId(ShapeId.fromParts("test", "MyOp"))
      .severity(Severity.ERROR)
      .message(
        "Operations annotated with @unpackedOutput must have an output structure with exactly one member, but test#MyOpOutput has 2"
      )
      .build()

    expect(events.contains(expected))
  }

  test(
    "UnpackedOutputTrait - error when operation has no explicit output"
  ) {
    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#unpackedOutput
          |
          |@unpackedOutput
          |operation MyOp {}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("UnpackedOutputTrait")
      .shapeId(ShapeId.fromParts("test", "MyOp"))
      .severity(Severity.ERROR)
      .message(
        "Operations annotated with @unpackedOutput must have an output structure with exactly one member, but smithy.api#Unit has 0"
      )
      .build()

    expect(events.contains(expected))
  }

  test(
    "UnpackedOutputTrait - cannot be applied to non-operation shapes"
  ) {
    val events = eventsWithoutLocations(
      assembleModel(
        """$version: "2"
          |namespace test
          |
          |use smithy4s.meta#unpackedOutput
          |
          |@unpackedOutput
          |structure NotAnOperation {
          |  item: String
          |}
          |""".stripMargin
      )
    )

    val expected = ValidationEvent
      .builder()
      .id("TraitTarget")
      .shapeId(ShapeId.fromParts("test", "NotAnOperation"))
      .severity(Severity.ERROR)
      .message(
        "Trait `smithy4s.meta#unpackedOutput` cannot be applied to `test#NotAnOperation`. This trait may only be applied to shapes that match the following selector: operation"
      )
      .build()

    expect(events.contains(expected))
  }
}
