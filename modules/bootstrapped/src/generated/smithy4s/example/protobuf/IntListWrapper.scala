package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class IntListWrapper(ints: List[Int])

object IntListWrapper extends ShapeTag.Companion[IntListWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "IntListWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(ints: List[Int]): IntListWrapper = IntListWrapper(ints)

  implicit val schema: Schema[IntListWrapper] = struct(
    IntList.underlyingSchema.required[IntListWrapper]("ints", _.ints),
  )(make).withId(id).addHints(hints)
}
