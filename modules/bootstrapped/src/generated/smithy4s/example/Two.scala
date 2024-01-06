package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int

final case class Two(value: Option[Int] = None)

object Two extends ShapeTag.Companion[Two] {
  val id: ShapeId = ShapeId("smithy4s.example", "Two")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Two] = struct(
    int.optional[Two]("value", _.value),
  ){
    Two.apply
  }.withId(id).addHints(hints)
}
