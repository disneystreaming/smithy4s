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

package smithy4s.dynamic

import smithy4s.ShapeId
import smithy4s.schema.Schema.EnumerationSchema
import munit.Location
import smithy4s.schema.EnumValue
import smithy4s.Hints
import smithy4s.Document
import smithy4s.schema.Schema
import smithy4s.schema.SchemaVisitor
import smithy4s.schema.EnumTag
import org.scalacheck.Prop
import org.scalacheck.Gen

class EnumSpec extends DummyIO.Suite {
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

    @deprecated
    enum EnumWithTraits {
      @deprecated ICE,
      FIRE
    }

    @alloy#openEnum
    enum OpenStringEnum {
      ICE,
      FIRE
    }

    @alloy#openEnum
    intEnum OpenIntEnum {
      ICE = 42,
      FIRE = 10
    }

    @enum([
      { value: "Vanilla" },
      { value: "Ice" }
    ])
    @alloy#openEnum
    string Open10Enum
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

  test("Smithy 2.0 int enums are supported") {
    assertEnum(
      ShapeId("example", "MyIntEnum"),
      expectedValues = List(
        EnumValue(
          stringValue = "ICE",
          intValue = 42,
          value = 42,
          name = "ICE",
          hints = Hints.empty
        ),
        EnumValue(
          stringValue = "FIRE",
          intValue = 10,
          value = 10,
          name = "FIRE",
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

      assertEquals(actual, Document.DString("Fire"))
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

  test("Smithy 2.0 enum members get their hints compiled") {
    assertEnum(
      ShapeId("example", "EnumWithTraits"),
      expectedValues = List(
        EnumValue(
          stringValue = "ICE",
          intValue = 0,
          value = 0,
          name = "ICE",
          hints = Hints(
            ShapeId("smithy.api", "deprecated") -> Document.obj()
          )
        ),
        EnumValue(
          stringValue = "FIRE",
          intValue = 1,
          value = 1,
          name = "FIRE",
          hints = Hints.empty
        )
      )
    )
  }

  test("Smithy 2.0 open string enums can be decoded to UNKNOWN") {
    compiled.map { index =>
      val schema = index
        .getSchema(ShapeId("example", "OpenStringEnum"))
        .getOrElse(fail("Error: shape missing"))

      decodeEncodeCheck(schema)(Document.fromString("not a known value"))
    }
  }

  test("Smithy 2.0 open int enums can be decoded to UNKNOWN") {
    compiled.map { index =>
      val schema = index
        .getSchema(ShapeId("example", "OpenIntEnum"))
        .getOrElse(fail("Error: shape missing"))

      decodeEncodeCheck(schema)(Document.fromInt(52))
    }
  }

  test("Smithy 1.0 open string enums can be decoded to UNKNOWN") {
    compiled.map { index =>
      val schema = index
        .getSchema(ShapeId("example", "Open10Enum"))
        .getOrElse(fail("Error: shape missing"))

      decodeEncodeCheck(schema)(Document.fromString("not a known value"))
    }
  }

  property("Enums members retain the ordering from the smithy specification") {
    // custom input to avoid scalacheck shrinking
    case class Input(identifiers: List[String])
    Prop.forAll(
      Gen
        .nonEmptyListOf(Gen.identifier.map(_.toUpperCase()))
        .map(_.distinct)
        .map(Input(_))
    ) { input =>
      import input.identifiers
      val stringEnumBuilder = software.amazon.smithy.model.shapes.EnumShape
        .builder()
        .id("input#MyStringEnum")

      val intEnumBuilder = software.amazon.smithy.model.shapes.IntEnumShape
        .builder()
        .id("input#MyIntEnum")
      identifiers.foreach(id => stringEnumBuilder.addMember(id, id))
      identifiers.zipWithIndex.foreach { case (name, value) =>
        intEnumBuilder.addMember(name, identifiers.size - value)
      }
      val stringEnumShape = stringEnumBuilder.build()
      val intEnumShape = intEnumBuilder.build()
      val unitShape = software.amazon.smithy.model.shapes.StructureShape
        .builder()
        .id("smithy.api#Unit")
        .build()

      val model = software.amazon.smithy.model.Model
        .builder()
        .addShapes(unitShape, stringEnumShape, intEnumShape)
        .build()

      val index: DynamicSchemaIndex = DynamicSchemaIndex.loadModel(model)
      val stringEnumSchemaNames = index
        .getSchema(ShapeId("input", "MyStringEnum"))
        .toList
        .flatMap(EnumNamesSchemaVisitor(_))

      val intEnumSchemaNames = index
        .getSchema(ShapeId("input", "MyIntEnum"))
        .toList
        .flatMap(EnumNamesSchemaVisitor(_))
      assertEquals(stringEnumSchemaNames, identifiers)
      assertEquals(intEnumSchemaNames, identifiers)
    }
  }

  type ConstList[A] = List[String]
  object EnumNamesSchemaVisitor extends SchemaVisitor.Default[ConstList] {
    def default[A]: List[String] = List.empty
    override def enumeration[E](
        shapeId: ShapeId,
        hints: Hints,
        tag: EnumTag[E],
        values: List[EnumValue[E]],
        total: E => EnumValue[E]
    ): ConstList[E] = values.map(_.name)
  }

  private def decodeEncodeCheck[A](schema: Schema[A])(input: Document) = {
    val decoded = Document.Decoder
      .fromSchema(schema)
      .decode(input)
      .toTry
      .get

    val encoded = Document.Encoder.fromSchema(schema).encode(decoded)

    assertEquals(encoded, input)
  }
}
