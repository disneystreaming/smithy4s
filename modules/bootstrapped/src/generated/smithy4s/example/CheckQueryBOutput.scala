package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class CheckQueryBOutput(variant: String)

object CheckQueryBOutput extends ShapeTag.Companion[CheckQueryBOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CheckQueryBOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(variant: String): CheckQueryBOutput = CheckQueryBOutput(variant)

  implicit val schema: Schema[CheckQueryBOutput] = struct(
    string.required[CheckQueryBOutput]("variant", _.variant).addHints(smithy.api.HttpPayload()),
  )(make).withId(id).addHints(hints)
}
