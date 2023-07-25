package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ListCitiesInput(nextToken: Option[String] = None, pageSize: Option[Int] = None)
object ListCitiesInput extends ShapeTag.Companion[ListCitiesInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ListCitiesInput")

  val hints: Hints = Hints.empty

  val nextToken = string.optional[ListCitiesInput]("nextToken", _.nextToken)
  val pageSize = int.optional[ListCitiesInput]("pageSize", _.pageSize)

  implicit val schema: Schema[ListCitiesInput] = struct(
    nextToken,
    pageSize,
  ){
    ListCitiesInput.apply
  }.withId(id).addHints(hints)
}
