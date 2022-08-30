package smithy4s.example

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class OptionInput(value: StringValue)
object OptionInput extends ShapeTag.Companion[OptionInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "OptionInput")

  val hints : Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[OptionInput] = struct(
    StringValue.schema.required[OptionInput]("value", _.value).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
  ){
    OptionInput.apply
  }.withId(id).addHints(hints)
}