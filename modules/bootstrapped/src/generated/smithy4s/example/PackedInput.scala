package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class PackedInput(key: String)
object PackedInput extends ShapeTag.Companion[PackedInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "PackedInput")

  val hints: Hints = Hints.empty

  object Optics {
    val key = Lens[PackedInput, String](_.key)(n => a => a.copy(key = n))
  }

  implicit val schema: Schema[PackedInput] = struct(
    string.required[PackedInput]("key", _.key).addHints(smithy.api.Required()),
  ){
    PackedInput.apply
  }.withId(id).addHints(hints)
}
