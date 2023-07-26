package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetCityOutput(name: String, coordinates: CityCoordinates)
object GetCityOutput extends ShapeTag.Companion[GetCityOutput] {

  val name = string.required[GetCityOutput]("name", _.name, n => c => c.copy(name = n)).addHints(Required())
  val coordinates = CityCoordinates.schema.required[GetCityOutput]("coordinates", _.coordinates, n => c => c.copy(coordinates = n)).addHints(Required())

  implicit val schema: Schema[GetCityOutput] = struct(
    name,
    coordinates,
  ){
    GetCityOutput.apply
  }
  .withId(ShapeId("smithy4s.example", "GetCityOutput"))
  .addHints(
    Hints.empty
  )
}
