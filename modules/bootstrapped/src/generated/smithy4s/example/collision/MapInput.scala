package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class MapInput(value: Map[String, String])

object MapInput extends ShapeTag.Companion[MapInput] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "MapInput")

  val hints: Hints = Hints.lazily(
    Hints(
      smithy.api.Input(),
    )
  )

  implicit val schema: Schema[MapInput] = struct(
    MyMap.underlyingSchema.required[MapInput]("value", _.value),
  ){
    MapInput.apply
  }.withId(id).addHints(hints)
}
