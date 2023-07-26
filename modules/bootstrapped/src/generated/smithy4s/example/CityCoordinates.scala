package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.struct

final case class CityCoordinates(latitude: Float, longitude: Float)
object CityCoordinates extends ShapeTag.Companion[CityCoordinates] {

  val latitude = float.required[CityCoordinates]("latitude", _.latitude, n => c => c.copy(latitude = n)).addHints(Required())
  val longitude = float.required[CityCoordinates]("longitude", _.longitude, n => c => c.copy(longitude = n)).addHints(Required())

  implicit val schema: Schema[CityCoordinates] = struct(
    latitude,
    longitude,
  ){
    CityCoordinates.apply
  }
  .withId(ShapeId("smithy4s.example", "CityCoordinates"))
  .addHints(
    Hints.empty
  )
}
