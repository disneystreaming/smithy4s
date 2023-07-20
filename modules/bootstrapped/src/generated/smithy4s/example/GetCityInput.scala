package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.struct

final case class GetCityInput(cityId: CityId)
object GetCityInput extends ShapeTag.Companion[GetCityInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetCityInput")

  val hints: Hints = Hints.empty

  object Optics {
    val cityIdLens = Lens[GetCityInput, CityId](_.cityId)(n => a => a.copy(cityId = n))
  }

  implicit val schema: Schema[GetCityInput] = struct(
    CityId.schema.required[GetCityInput]("cityId", _.cityId).addHints(smithy.api.Required()),
  ){
    GetCityInput.apply
  }.withId(id).addHints(hints)
}
