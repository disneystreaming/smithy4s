package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class CheckQueryAOutput(variant: String)

object CheckQueryAOutput extends ShapeTag.Companion[CheckQueryAOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CheckQueryAOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(variant: String): CheckQueryAOutput = CheckQueryAOutput(variant)

  implicit val schema: Schema[CheckQueryAOutput] = struct(
    string.required[CheckQueryAOutput]("variant", _.variant).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
