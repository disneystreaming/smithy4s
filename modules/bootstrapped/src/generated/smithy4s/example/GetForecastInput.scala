package smithy4s.example

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class GetForecastInput(cityId: CityId)
object GetForecastInput extends ShapeTag.Companion[GetForecastInput] {

  val cityId: FieldLens[GetForecastInput, CityId] = CityId.schema.required[GetForecastInput]("cityId", _.cityId, n => c => c.copy(cityId = n)).addHints(Required())

  implicit val schema: Schema[GetForecastInput] = struct(
    cityId,
  ){
    GetForecastInput.apply
  }
  .withId(ShapeId("smithy4s.example", "GetForecastInput"))
}
