package smithy4s.example.collision

import smithy.api.Input
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class MapInput(value: Map[String, String])
object MapInput extends ShapeTag.Companion[MapInput] {

  val value = MyMap.underlyingSchema.required[MapInput]("value", _.value, n => c => c.copy(value = n)).addHints(Required())

  implicit val schema: Schema[MapInput] = struct(
    value,
  ){
    MapInput.apply
  }
  .withId(ShapeId("smithy4s.example.collision", "MapInput"))
  .addHints(
    Hints(
      Input(),
    )
  )
}
