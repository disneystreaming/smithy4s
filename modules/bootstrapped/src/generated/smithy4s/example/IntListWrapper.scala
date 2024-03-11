package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class IntListWrapper(ints: IntList)

object IntListWrapper extends ShapeTag.Companion[IntListWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example", "IntListWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(ints: IntList): IntListWrapper = IntListWrapper(ints)

  implicit val schema: Schema[IntListWrapper] = struct(
    IntList.schema.required[IntListWrapper]("ints", _.ints).addHints(alloy.proto.ProtoIndex(1)),
  )(make).withId(id).addHints(hints)
}
