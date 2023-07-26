package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class GetCityInput(cityId: CityId)
object GetCityInput extends ShapeTag.$Companion[GetCityInput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "GetCityInput")

  val $hints: Hints = Hints.empty

  val cityId: FieldLens[GetCityInput, CityId] = CityId.$schema.required[GetCityInput]("cityId", _.cityId, n => c => c.copy(cityId = n)).addHints(Required())

  implicit val $schema: Schema[GetCityInput] = struct(
    cityId,
  ){
    GetCityInput.apply
  }.withId($id).addHints($hints)
}
