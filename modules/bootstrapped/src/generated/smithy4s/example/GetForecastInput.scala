package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetForecastInput(cityId: CityId)
object GetForecastInput extends ShapeTag.Companion[GetForecastInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetForecastInput")

  val hints: Hints = Hints.empty

  val cityId = CityId.schema.required[GetForecastInput]("cityId", _.cityId).addHints(smithy.api.Required())

  implicit val schema: Schema[GetForecastInput] = struct(
    cityId,
  ){
    GetForecastInput.apply
  }.withId(id).addHints(hints)
}
