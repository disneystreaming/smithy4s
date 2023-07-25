package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class PackedInput(key: String)
object PackedInput extends ShapeTag.Companion[PackedInput] {
  val hints: Hints = Hints.empty

  val key = string.required[PackedInput]("key", _.key).addHints(smithy.api.Required())

  implicit val schema: Schema[PackedInput] = struct(
    key,
  ){
    PackedInput.apply
  }.withId(ShapeId("smithy4s.example", "PackedInput")).addHints(hints)
}
