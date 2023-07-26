package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class One(value: Option[String] = None)
object One extends ShapeTag.Companion[One] {

  val value = string.optional[One]("value", _.value, n => c => c.copy(value = n))

  implicit val schema: Schema[One] = struct(
    value,
  ){
    One.apply
  }
  .withId(ShapeId("smithy4s.example", "One"))
  .addHints(
    Hints.empty
  )
}
