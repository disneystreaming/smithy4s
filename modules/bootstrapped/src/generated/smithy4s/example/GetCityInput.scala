package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.optics.Lens

final case class GetCityInput(cityId: CityId)

object GetCityInput extends ShapeTag.Companion[GetCityInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetCityInput")

  val hints: Hints = Hints.empty

  object optics {
    val cityId: Lens[GetCityInput, CityId] = Lens[GetCityInput, CityId](_.cityId)(n => a => a.copy(cityId = n))
  }

  implicit val schema: Schema[GetCityInput] = struct(
    CityId.schema.required[GetCityInput]("cityId", _.cityId),
  ){
    GetCityInput.apply
  }.withId(id).addHints(hints)
}
