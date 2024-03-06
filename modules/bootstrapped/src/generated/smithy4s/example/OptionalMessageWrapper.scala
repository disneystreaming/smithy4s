package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class OptionalMessageWrapper(message: Option[Integers] = None)

object OptionalMessageWrapper extends ShapeTag.Companion[OptionalMessageWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example", "OptionalMessageWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: Option[Integers]): OptionalMessageWrapper = OptionalMessageWrapper(message)

  implicit val schema: Schema[OptionalMessageWrapper] = struct(
    Integers.schema.optional[OptionalMessageWrapper]("message", _.message).addHints(alloy.proto.ProtoIndex(1)),
  )(make).withId(id).addHints(hints)
}
