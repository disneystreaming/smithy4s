package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GetCityOutput(name: String, coordinates: CityCoordinates)

object GetCityOutput extends ShapeTag.Companion[GetCityOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetCityOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetCityOutput] = struct(
    string.required[GetCityOutput]("name", _.name),
    CityCoordinates.schema.required[GetCityOutput]("coordinates", _.coordinates),
  ){
    GetCityOutput.apply
  }.withId(id).addHints(hints)
}
