package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.struct

final case class CityCoordinates(latitude: Float, longitude: Float)
object CityCoordinates extends ShapeTag.Companion[CityCoordinates] {
  val id: ShapeId = ShapeId("smithy4s.example", "CityCoordinates")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[CityCoordinates] = struct(
    float.required[CityCoordinates]("latitude", _.latitude),
    float.required[CityCoordinates]("longitude", _.longitude),
  ){
    CityCoordinates.apply
  }.withId(id).addHints(hints)
}
