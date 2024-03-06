package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class MessageWrapper(message: Integers)

object MessageWrapper extends ShapeTag.Companion[MessageWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example", "MessageWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: Integers): MessageWrapper = MessageWrapper(message)

  implicit val schema: Schema[MessageWrapper] = struct(
    Integers.schema.required[MessageWrapper]("message", _.message).addHints(alloy.proto.ProtoIndex(1)),
  )(make).withId(id).addHints(hints)
}
