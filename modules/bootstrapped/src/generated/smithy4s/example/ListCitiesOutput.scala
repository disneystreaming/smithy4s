package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ListCitiesOutput(items: List[CitySummary], nextToken: Option[String] = None)

object ListCitiesOutput extends ShapeTag.Companion[ListCitiesOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ListCitiesOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(nextToken: Option[String], items: List[CitySummary]): ListCitiesOutput = ListCitiesOutput(items, nextToken)

  implicit val schema: Schema[ListCitiesOutput] = struct(
    string.optional[ListCitiesOutput]("nextToken", _.nextToken),
    CitySummaries.underlyingSchema.required[ListCitiesOutput]("items", _.items),
  )(make).withId(id).addHints(hints)
}
