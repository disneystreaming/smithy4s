package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string

final case class ListCitiesInput(nextToken: Option[String] = None, pageSize: Option[Int] = None)

object ListCitiesInput extends ShapeTag.Companion[ListCitiesInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ListCitiesInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[ListCitiesInput] = struct(
    string.optional[ListCitiesInput]("nextToken", _.nextToken),
    int.optional[ListCitiesInput]("pageSize", _.pageSize),
  ){
    ListCitiesInput.apply
  }.withId(id).addHints(hints)
}
