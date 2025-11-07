package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class Enums(closedString: ClosedString, openString: OpenString, closedInt: ClosedInt, openInt: OpenInt)

object Enums extends ShapeTag.Companion[Enums] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "Enums")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(closedString: ClosedString, openString: OpenString, closedInt: ClosedInt, openInt: OpenInt): Enums = Enums(closedString, openString, closedInt, openInt)

  implicit val schema: Schema[Enums] = struct(
    ClosedString.schema.required[Enums]("closedString", _.closedString),
    OpenString.schema.required[Enums]("openString", _.openString),
    ClosedInt.schema.required[Enums]("closedInt", _.closedInt),
    OpenInt.schema.required[Enums]("openInt", _.openInt),
  )(make).withId(id).addHints(hints)
}
