package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class CitySummary(cityId: CityId, name: String)

object CitySummary extends ShapeTag.Companion[CitySummary] {
  val id: ShapeId = ShapeId("smithy4s.example", "CitySummary")

  val hints: Hints = Hints(
    smithy.api.References(List(smithy.api.Reference(resource = smithy.api.NonEmptyString("smithy4s.example#City"), ids = None, service = None, rel = None))),
  )

  implicit val schema: Schema[CitySummary] = struct(
    CityId.schema.required[CitySummary]("cityId", _.cityId),
    string.required[CitySummary]("name", _.name),
  ){
    CitySummary.apply
  }.withId(id).addHints(hints)
}
