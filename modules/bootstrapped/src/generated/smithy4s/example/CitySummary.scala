package smithy4s.example

import smithy.api.NonEmptyString
import smithy.api.Reference
import smithy.api.References
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class CitySummary(cityId: CityId, name: String)
object CitySummary extends ShapeTag.$Companion[CitySummary] {
  val $id: ShapeId = ShapeId("smithy4s.example", "CitySummary")

  val $hints: Hints = Hints(
    References(List(Reference(resource = NonEmptyString("smithy4s.example#City"), ids = None, service = None, rel = None))),
  )

  val cityId: FieldLens[CitySummary, CityId] = CityId.$schema.required[CitySummary]("cityId", _.cityId, n => c => c.copy(cityId = n)).addHints(Required())
  val name: FieldLens[CitySummary, String] = string.required[CitySummary]("name", _.name, n => c => c.copy(name = n)).addHints(Required())

  implicit val $schema: Schema[CitySummary] = struct(
    cityId,
    name,
  ){
    CitySummary.apply
  }.withId($id).addHints($hints)
}
