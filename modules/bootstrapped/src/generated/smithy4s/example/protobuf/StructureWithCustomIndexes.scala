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
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(a: Option[Int], b: Int, c: Int, d: Option[UnionWithCustomIndexes]): StructureWithCustomIndexes = StructureWithCustomIndexes(c, b, a, d)

  implicit val schema: Schema[StructureWithCustomIndexes] = struct(
    int.optional[StructureWithCustomIndexes]("a", _.a).addHints(alloy.proto.ProtoIndex(4)),
    int.field[StructureWithCustomIndexes]("b", _.b).addHints(smithy.api.Default(smithy4s.Document.fromDouble(0.0d)), alloy.proto.ProtoIndex(3)),
    int.required[StructureWithCustomIndexes]("c", _.c).addHints(alloy.proto.ProtoIndex(2)),
    UnionWithCustomIndexes.schema.optional[StructureWithCustomIndexes]("d", _.d).addHints(alloy.proto.ProtoIndex(1)),
  )(make).withId(id).addHints(hints)
}
