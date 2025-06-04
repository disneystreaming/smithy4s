package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class CheckQueryOutput(variant: String)

object CheckQueryOutput extends ShapeTag.Companion[CheckQueryOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CheckQueryOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(variant: String): CheckQueryOutput = CheckQueryOutput(variant)

  implicit val schema: Schema[CheckQueryOutput] = struct(
    string.required[CheckQueryOutput]("variant", _.variant).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
