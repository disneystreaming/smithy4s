package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.float
import smithy4s.schema.Schema.struct

final case class CityCoordinates(latitude: Float, longitude: Float)
object CityCoordinates extends ShapeTag.Companion[CityCoordinates] {
  val id: ShapeId = ShapeId("smithy4s.example", "CityCoordinates")

  val hints: Hints = Hints.empty

  object Optics {
    val latitude = Lens[CityCoordinates, Float](_.latitude)(n => a => a.copy(latitude = n))
    val longitude = Lens[CityCoordinates, Float](_.longitude)(n => a => a.copy(longitude = n))
  }

  implicit val schema: Schema[CityCoordinates] = struct(
    float.required[CityCoordinates]("latitude", _.latitude).addHints(smithy.api.Required()),
    float.required[CityCoordinates]("longitude", _.longitude).addHints(smithy.api.Required()),
  ){
    CityCoordinates.apply
  }.withId(id).addHints(hints)
}
