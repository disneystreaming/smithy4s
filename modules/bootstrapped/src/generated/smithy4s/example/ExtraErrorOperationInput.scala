package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ExtraErrorOperationInput(in: Option[String] = None)
object ExtraErrorOperationInput extends ShapeTag.Companion[ExtraErrorOperationInput] {
  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  val in = string.optional[ExtraErrorOperationInput]("in", _.in)

  implicit val schema: Schema[ExtraErrorOperationInput] = struct(
    in,
  ){
    ExtraErrorOperationInput.apply
  }.withId(ShapeId("smithy4s.example", "ExtraErrorOperationInput")).addHints(hints)
}
