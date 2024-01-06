package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.recursive
import _root_.smithy4s.schema.Schema.struct

final case class RecursiveInput(hello: Option[smithy4s.example.RecursiveInput] = None)

object RecursiveInput extends ShapeTag.Companion[RecursiveInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[RecursiveInput] = recursive(struct(
    smithy4s.example.RecursiveInput.schema.optional[RecursiveInput]("hello", _.hello),
  ){
    RecursiveInput.apply
  }.withId(id).addHints(hints))
}
