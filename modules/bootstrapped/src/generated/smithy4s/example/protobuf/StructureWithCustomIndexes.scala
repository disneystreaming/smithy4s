package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class StructureWithCustomIndexes(c: Int, b: Int = 0, a: Option[Int] = None, d: Option[UnionWithCustomIndexes] = None)

object StructureWithCustomIndexes extends ShapeTag.Companion[StructureWithCustomIndexes] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "StructureWithCustomIndexes")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(a: Option[Int], b: Int, c: Int, d: Option[UnionWithCustomIndexes]): StructureWithCustomIndexes = StructureWithCustomIndexes(c, b, a, d)

  implicit val schema: Schema[StructureWithCustomIndexes] = struct(
    int.optional[StructureWithCustomIndexes]("a", _.a).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoIndex"), smithy4s.Document.fromDouble(4.0d))),
    int.field[StructureWithCustomIndexes]("b", _.b).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoIndex"), smithy4s.Document.fromDouble(3.0d)), Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromDouble(0.0d))),
    int.required[StructureWithCustomIndexes]("c", _.c).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoIndex"), smithy4s.Document.fromDouble(2.0d))),
    UnionWithCustomIndexes.schema.optional[StructureWithCustomIndexes]("d", _.d).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoIndex"), smithy4s.Document.fromDouble(1.0d))),
  )(make).withId(id).addHints(hints)
}
