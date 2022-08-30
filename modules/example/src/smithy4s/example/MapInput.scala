package smithy4s.example

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.struct
import smithy4s.ShapeTag

case class MapInput(value: Map[StringKey,StringValue])
object MapInput extends ShapeTag.Companion[MapInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "MapInput")

  val hints : Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[MapInput] = struct(
    MyMap.underlyingSchema.required[MapInput]("value", _.value).addHints(smithy.api.Required()),
  ){
    MapInput.apply
  }.withId(id).addHints(hints)
}