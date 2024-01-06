package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.recursive
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int

final case class IntList(head: Int, tail: Option[smithy4s.example.IntList] = None)

object IntList extends ShapeTag.Companion[IntList] {
  val id: ShapeId = ShapeId("smithy4s.example", "IntList")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[IntList] = recursive(struct(
    int.required[IntList]("head", _.head),
    smithy4s.example.IntList.schema.optional[IntList]("tail", _.tail),
  ){
    IntList.apply
  }.withId(id).addHints(hints))
}
