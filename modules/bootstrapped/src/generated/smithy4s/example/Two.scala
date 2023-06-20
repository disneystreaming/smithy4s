package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

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
