package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class Two(value: Option[Int] = None)
object Two extends ShapeTag.Companion[Two] {

  val value = int.optional[Two]("value", _.value, n => c => c.copy(value = n))

  implicit val schema: Schema[Two] = struct(
    value,
  ){
    Two.apply
  }
  .withId(ShapeId("smithy4s.example", "Two"))
  .addHints(
    Hints.empty
  )
}
