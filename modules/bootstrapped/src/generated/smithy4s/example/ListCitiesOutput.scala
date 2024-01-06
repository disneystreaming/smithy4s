package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class ListCitiesOutput(items: List[CitySummary], nextToken: Option[String] = None)

object ListCitiesOutput extends ShapeTag.Companion[ListCitiesOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ListCitiesOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[ListCitiesOutput] = struct(
    CitySummaries.underlyingSchema.required[ListCitiesOutput]("items", _.items),
    string.optional[ListCitiesOutput]("nextToken", _.nextToken),
  ){
    ListCitiesOutput.apply
  }.withId(id).addHints(hints)
}
