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

  implicit val schema: Schema[StructureWithCustomIndexes] = struct[StructureWithCustomIndexes](
    int.optional[StructureWithCustomIndexes]("a", _.a).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoIndex"), smithy4s.Document.fromLong(4L))),
    int.field[StructureWithCustomIndexes]("b", _.b).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoIndex"), smithy4s.Document.fromLong(3L)), Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromLong(0L))),
    int.required[StructureWithCustomIndexes]("c", _.c).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoIndex"), smithy4s.Document.fromLong(2L))),
    UnionWithCustomIndexes.schema.optional[StructureWithCustomIndexes]("d", _.d).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoIndex"), smithy4s.Document.fromLong(1L))),
  )(make).withId(id).addHints(hints)
}
