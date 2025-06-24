package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class AbcDefOutput(message: Option[String] = None)

object AbcDefOutput extends ShapeTag.Companion[AbcDefOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "AbcDefOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(message: Option[String]): AbcDefOutput = AbcDefOutput(message)

  implicit val schema: Schema[AbcDefOutput] = struct(
    string.optional[AbcDefOutput]("message", _.message).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
