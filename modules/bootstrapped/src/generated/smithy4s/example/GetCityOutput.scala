package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetCityOutput(name: String, coordinates: CityCoordinates)
object GetCityOutput extends ShapeTag.Companion[GetCityOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetCityOutput")

  val hints: Hints = Hints.empty

  object Lenses {
    val name = Lens[GetCityOutput, String](_.name)(n => a => a.copy(name = n))
    val coordinates = Lens[GetCityOutput, CityCoordinates](_.coordinates)(n => a => a.copy(coordinates = n))
  }

  implicit val schema: Schema[GetCityOutput] = struct(
    string.required[GetCityOutput]("name", _.name).addHints(smithy.api.Required()),
    CityCoordinates.schema.required[GetCityOutput]("coordinates", _.coordinates).addHints(smithy.api.Required()),
  ){
    GetCityOutput.apply
  }.withId(id).addHints(hints)
}
