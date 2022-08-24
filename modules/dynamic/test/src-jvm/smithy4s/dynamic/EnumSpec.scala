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

package smithy4s.dynamic

import munit.FunSuite
import DummyIO._
import smithy4s.ShapeId
import smithy4s.schema.Schema.EnumerationSchema
import munit.Location
import smithy4s.schema.EnumValue
import smithy4s.Hints
import smithy4s.Document
import smithy4s.schema.Schema
import smithy4s.IntEnum

class EnumSpec extends FunSuite {
  val model = """
    $version: "2"
    namespace example

    @enum([
      { value: "Ice", name: "ICE" },
      { value: "Fire", name: "FIRE" }
    ])
    string Element

    @enum([
      { value: "Vanilla" },
      { value: "Ice" }
    ])
    string AnonymousElement

    enum Smithy20Enum {
      @enumValue("Ice")
      ICE

      FIRE = "Fire"
    }

    intEnum MyIntEnum {
      @enumValue(42)
      ICE

      FIRE = 10
    }
  """

  val compiled = Utils.compile(model)

  def assertEnum(
      shapeId: ShapeId,
      expectedValues: List[EnumValue[_]]
  )(implicit
      loc: Location
  ) = {
    compiled
      .map { index =>
        val schema = index
          .getSchema(shapeId)
          .getOrElse(fail("Error: shape missing"))

        val eValues = schema match {
          case e: EnumerationSchema[_] =>
            e.values.map(_.asInstanceOf[EnumValue[_]])
          case unexpected => fail("Unexpected schema: " + unexpected)
        }

        assertEquals(eValues, expectedValues)
      }
      .check()
  }

  test("dynamic enums have names if they're in the model") {
    assertEnum(
      ShapeId("example", "Element"),
      expectedValues = List(
        EnumValue(
          stringValue = "Ice",
          intValue = 0,
          value = 0,
          name = "ICE",
          hints = Hints.empty
        ),
        EnumValue(
          stringValue = "Fire",
          intValue = 1,
          value = 1,
          name = "FIRE",
          hints = Hints.empty
        )
      )
    )
  }

  test("dynamic enum names are derived if not present in the model") {
    assertEnum(
      ShapeId("example", "AnonymousElement"),
      expectedValues = List(
        EnumValue(
          stringValue = "Vanilla",
          intValue = 0,
          value = 0,
          name = "VANILLA",
          hints = Hints.empty
        ),
        EnumValue(
          stringValue = "Ice",
          intValue = 1,
          value = 1,
          name = "ICE",
          hints = Hints.empty
        )
      )
    )
  }

  test("Smithy 2.0 enums are supported") {
    assertEnum(
      ShapeId("example", "Smithy20Enum"),
      expectedValues = List(
        EnumValue(
          stringValue = "Fire",
          intValue = 0,
          value = 0,
          name = "FIRE",
          hints = Hints.empty
        ),
        EnumValue(
          stringValue = "Ice",
          intValue = 1,
          value = 1,
          name = "ICE",
          hints = Hints.empty
        )
      )
    )
  }

  test("Smithy 2.0 int enums are supported") {
    assertEnum(
      ShapeId("example", "MyIntEnum"),
      expectedValues = List(
        EnumValue(
          stringValue = "FIRE",
          intValue = 10,
          value = 10,
          name = "FIRE",
          hints = Hints.empty
        ),
        EnumValue(
          stringValue = "ICE",
          intValue = 42,
          value = 42,
          name = "ICE",
          hints = Hints.empty
        )
      )
    )
  }

  test("Smithy 2.0 string enums are converted to string documents") {
    compiled.map { index =>
      val actual = Document.Encoder
        .fromSchema(
          index
            .getSchema(ShapeId("example", "Smithy20Enum"))
            .getOrElse(fail("Error: shape missing"))
            .asInstanceOf[Schema[Int]]
        )
        .encode(1)

      assertEquals(actual, Document.DString("Ice"))
    }
  }

  test("Smithy 2.0 int enums have the IntEnum trait") {
    compiled.map { index =>
      val hint = index
        .getSchema(ShapeId("example", "MyIntEnum"))
        .getOrElse(fail("Error: shape missing"))
        .hints
        .get(IntEnum)

      assertEquals(hint, Some(IntEnum()))
    }
  }

  test("Smithy 2.0 int enums are converted to int documents") {
    compiled.map { index =>
      val ICE = 42

      val actual = Document.Encoder
        .fromSchema(
          index
            .getSchema(ShapeId("example", "MyIntEnum"))
            .getOrElse(fail("Error: shape missing"))
            .asInstanceOf[Schema[Int]]
        )
        .encode(ICE)

      assertEquals(actual, Document.DNumber(ICE))
    }
  }
}
