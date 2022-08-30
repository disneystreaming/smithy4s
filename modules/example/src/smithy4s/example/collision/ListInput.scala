package smithy4s.example.collision

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class ListInput(value: String)
object ListInput extends ShapeTag.Companion[ListInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "ListInput")

  val hints : Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[ListInput] = struct(
    _String.underlyingSchema.required[ListInput]("value", _.value).addHints(smithy.api.HttpLabel(), smithy.api.Required()),
  ){
    ListInput.apply
  }.withId(id).addHints(hints)
}