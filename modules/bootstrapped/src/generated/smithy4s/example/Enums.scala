package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class Enums(closedString: ClosedString, openString: OpenString, closedInt: ClosedInt, openInt: OpenInt)

object Enums extends ShapeTag.Companion[Enums] {
  val id: ShapeId = ShapeId("smithy4s.example", "Enums")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(closedString: ClosedString, openString: OpenString, closedInt: ClosedInt, openInt: OpenInt): Enums = Enums(closedString, openString, closedInt, openInt)

  implicit val schema: Schema[Enums] = struct(
    ClosedString.schema.required[Enums]("closedString", _.closedString).addHints(alloy.proto.ProtoIndex(1)),
    OpenString.schema.required[Enums]("openString", _.openString).addHints(alloy.proto.ProtoIndex(2)),
    ClosedInt.schema.required[Enums]("closedInt", _.closedInt).addHints(alloy.proto.ProtoIndex(3)),
    OpenInt.schema.required[Enums]("openInt", _.openInt).addHints(alloy.proto.ProtoIndex(4)),
  )(make).withId(id).addHints(hints)
}
