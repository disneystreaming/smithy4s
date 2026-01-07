package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetCityOutput(name: String, coordinates: CityCoordinates)

object GetCityOutput extends ShapeTag.Companion[GetCityOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetCityOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(name: String, coordinates: CityCoordinates): GetCityOutput = GetCityOutput(name, coordinates)

  implicit val schema: Schema[GetCityOutput] = struct(
    string.required[GetCityOutput]("name", _.name),
    CityCoordinates.schema.required[GetCityOutput]("coordinates", _.coordinates),
  )(make).withId(id).addHints(hints)
}
