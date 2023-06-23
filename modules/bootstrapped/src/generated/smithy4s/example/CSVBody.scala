package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class CSVBody(csv: CSV)
object CSVBody extends ShapeTag.Companion[CSVBody] {
  val id: ShapeId = ShapeId("smithy4s.example", "CSVBody")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[CSVBody] = struct(
    CSV.schema.required[CSVBody]("csv", _.csv).addHints(smithy.api.HttpPayload(), smithy.api.Required()),
  ){
    CSVBody.apply
  }.withId(id).addHints(hints)
}
