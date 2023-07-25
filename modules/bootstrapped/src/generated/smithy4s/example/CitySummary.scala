package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class CitySummary(cityId: CityId, name: String)
object CitySummary extends ShapeTag.Companion[CitySummary] {
  val id: ShapeId = ShapeId("smithy4s.example", "CitySummary")

  val hints: Hints = Hints(
    smithy.api.References(List(smithy.api.Reference(resource = smithy.api.NonEmptyString("smithy4s.example#City"), ids = None, service = None, rel = None))),
  )

  val cityId = CityId.schema.required[CitySummary]("cityId", _.cityId).addHints(smithy.api.Required())
  val name = string.required[CitySummary]("name", _.name).addHints(smithy.api.Required())

  implicit val schema: Schema[CitySummary] = struct(
    cityId,
    name,
  ){
    CitySummary.apply
  }.withId(id).addHints(hints)
}
