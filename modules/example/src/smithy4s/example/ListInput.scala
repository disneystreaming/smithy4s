package smithy4s.example

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class ListInput(value: Value)
object ListInput extends ShapeTag.Companion[ListInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ListInput")

  val hints : Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[ListInput] = struct(
    Value.schema.required[ListInput]("value", _.value).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
  ){
    ListInput.apply
  }.withId(id).addHints(hints)
}