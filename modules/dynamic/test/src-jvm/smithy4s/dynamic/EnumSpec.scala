package smithy4s.dynamic

import munit.FunSuite
import DummyIO._
import smithy4s.ShapeId
import smithy4s.schema.Schema.EnumerationSchema
import munit.Location

class EnumSpec extends FunSuite {
  val model = """
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
  """

  def assertNames(shapeId: ShapeId, names: List[String])(implicit
      loc: Location
  ) = {
    Utils
      .compile(model)
      .map { index =>
        val schema = index
          .getSchema(shapeId)
          .getOrElse(fail("Error: shape missing"))

        val values = schema match {
          case e: EnumerationSchema[_] => e.values
          case unexpected => fail("Unexpected schema: " + unexpected)
        }

        assertEquals(values.map(_.name), names)
      }
      .check()
  }

  test("dynamic enums have names if they're in the model") {
    assertNames(ShapeId("example", "Element"), List("ICE", "FIRE"))
  }

  test("dynamic enum names are derived if not present in the model") {
    assertNames(ShapeId("example", "AnonymousElement"), List("VANILLA", "ICE"))
  }
}
