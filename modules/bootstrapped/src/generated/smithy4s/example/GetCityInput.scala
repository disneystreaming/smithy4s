package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetCityInput(cityId: CityId)
object GetCityInput extends ShapeTag.Companion[GetCityInput] {

  val cityId = CityId.schema.required[GetCityInput]("cityId", _.cityId, n => c => c.copy(cityId = n)).addHints(Required())

  implicit val schema: Schema[GetCityInput] = struct(
    cityId,
  ){
    GetCityInput.apply
  }
  .withId(ShapeId("smithy4s.example", "GetCityInput"))
  .addHints(
    Hints.empty
  )
}
