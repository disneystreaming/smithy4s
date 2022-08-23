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
import smithy4s.Document

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

  def assertEnum(shapeId: ShapeId, names: List[String], values: List[Any])(
      implicit loc: Location
  ) = {
    Utils
      .compile(model)
      .map { index =>
        val schema = index
          .getSchema(shapeId)
          .getOrElse(fail("Error: shape missing"))

        val eValues = schema match {
          case e: EnumerationSchema[_] =>
            e.values.collect {
              case EnumValue(
                    _,
                    _,
                    aValue,
                    name,
                    _
                  ) =>
                val value = aValue match {
                  case i: Integer          => i
                  case s: String           => s
                  case Document.DNumber(x) => x.toInt
                  case Document.DString(s) => s
                }
                name -> value
            }
          case unexpected => fail("Unexpected schema: " + unexpected)
        }

        assertEquals(eValues.map(_._1).sorted, names.sorted)
        assertEquals[List[Any], List[Any]](
          eValues.map(_._2),
          values
        )
      }
      .check()
  }

  test("dynamic enums have names if they're in the model") {
    assertEnum(
      ShapeId("example", "Element"),
      names = List("ICE", "FIRE"),
      values = List("Ice", "Fire")
    )
  }

  test("dynamic enum names are derived if not present in the model") {
    assertEnum(
      ShapeId("example", "AnonymousElement"),
      names = List("VANILLA", "ICE"),
      values = List("Vanilla", "Ice")
    )
  }

  test("Smithy 2.0 enums are supported") {
    assertEnum(
      ShapeId("example", "Smithy20Enum"),
      names = List("ICE", "FIRE"),
      values = List("Fire", "Ice")
    )
  }

  test("Smithy 2.0 int enums are supported") {
    assertEnum(
      ShapeId("example", "MyIntEnum"),
      names = List("ICE", "FIRE"),
      values = List(10, 42)
    )
  }
}
