package smithy4s.example.collision

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class OptionInput(value: _String)
object OptionInput extends ShapeTag.Companion[OptionInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "OptionInput")

  val hints : Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[OptionInput] = struct(
    _String.schema.required[OptionInput]("value", _.value).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
  ){
    OptionInput.apply
  }.withId(id).addHints(hints)
}