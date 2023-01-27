package smithy4s
import munit._

class ShapeIdParseSpec extends FunSuite {
  test("ShapeId can be parsed from valid shape IDs") {
    val shapeId = ShapeId.parse("smithy4s#ShapeId")
    expect.same(shapeId, Some(ShapeId("smithy4s", "ShapeId")))
  }

  test("ShapeId doesn't parse if no hash is present") {
    val shapeId = ShapeId.parse("smithy4sShapeId")
    expect(shapeId.isEmpty)
  }

  test("ShapeId doesn't parse if there's nothing before the hash") {
    val shapeId = ShapeId.parse("#ShapeId")
    expect(shapeId.isEmpty)
  }

  test("ShapeId doesn't parse if there's nothing after the hash") {
    val shapeId = ShapeId.parse("smithy4s#")
    expect(shapeId.isEmpty)
  }

  test("ShapeId doesn't parse if multiple hashes are present") {
    val shapeId = ShapeId.parse("smithy4s#Shape#Id")
    expect(shapeId.isEmpty)
  }
}
