package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class MessageOutput(message: String)

object MessageOutput extends ShapeTag.Companion[MessageOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "MessageOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(message: String): MessageOutput = MessageOutput(message)

  implicit val schema: Schema[MessageOutput] = struct(
    string.required[MessageOutput]("message", _.message).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
