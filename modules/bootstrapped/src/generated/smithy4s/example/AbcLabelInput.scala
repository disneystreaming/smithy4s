package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class AbcLabelInput(_def: String)

object AbcLabelInput extends ShapeTag.Companion[AbcLabelInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "AbcLabelInput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(_def: String): AbcLabelInput = AbcLabelInput(_def)

  implicit val schema: Schema[AbcLabelInput] = struct(
    string.required[AbcLabelInput]("def", _._def).addHints(smithy.api.HttpLabel()),
  )(make).withId(id).addHints(hints)
}
