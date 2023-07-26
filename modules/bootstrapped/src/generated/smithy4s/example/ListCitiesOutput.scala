package smithy4s.example

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ListCitiesOutput(items: List[CitySummary], nextToken: Option[String] = None)
object ListCitiesOutput extends ShapeTag.Companion[ListCitiesOutput] {

  val items: FieldLens[ListCitiesOutput, List[CitySummary]] = CitySummaries.underlyingSchema.required[ListCitiesOutput]("items", _.items, n => c => c.copy(items = n)).addHints(Required())
  val nextToken: FieldLens[ListCitiesOutput, Option[String]] = string.optional[ListCitiesOutput]("nextToken", _.nextToken, n => c => c.copy(nextToken = n))

  implicit val schema: Schema[ListCitiesOutput] = struct(
    items,
    nextToken,
  ){
    ListCitiesOutput.apply
  }
  .withId(ShapeId("smithy4s.example", "ListCitiesOutput"))
}
