package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class PackedInput(key: String)

object PackedInput extends ShapeTag.Companion[PackedInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "PackedInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[PackedInput] = struct(
    string.required[PackedInput]("key", _.key),
  ){
    PackedInput.apply
  }.withId(id).addHints(hints)
}
