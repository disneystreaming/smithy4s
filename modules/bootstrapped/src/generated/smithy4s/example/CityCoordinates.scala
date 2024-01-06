package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.float

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
