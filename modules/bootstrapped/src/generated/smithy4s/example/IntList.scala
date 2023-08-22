package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class IntList(head: Int, tail: Option[smithy4s.example.IntList] = None)
object IntList extends ShapeTag.Companion[IntList] {
  val id: ShapeId = ShapeId("smithy4s.example", "IntList")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[IntList] = recursive(struct(
    int.required[IntList]("head", _.head).addHints(smithy.api.Required()),
    smithy4s.example.IntList.schema.optional[IntList]("tail", _.tail),
  ){
    IntList.apply
  }.withId(id).addHints(hints))
}
