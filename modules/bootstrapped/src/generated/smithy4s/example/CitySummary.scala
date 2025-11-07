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
    Hints.dynamic(ShapeId("smithy.api", "references"), smithy4s.Document.array(smithy4s.Document.obj("resource" -> smithy4s.Document.fromString("smithy4s.example#City")))),
  )

  // constructor using the original order from the spec
  private def make(cityId: CityId, name: String): CitySummary = CitySummary(cityId, name)

  implicit val schema: Schema[CitySummary] = struct(
    CityId.schema.required[CitySummary]("cityId", _.cityId),
    string.required[CitySummary]("name", _.name),
  )(make).withId(id).addHints(hints)
}
