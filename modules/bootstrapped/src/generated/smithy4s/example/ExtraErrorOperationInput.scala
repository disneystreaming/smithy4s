package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ExtraErrorOperationInput(in: Option[String] = None)

object ExtraErrorOperationInput extends ShapeTag.Companion[ExtraErrorOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ExtraErrorOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[ExtraErrorOperationInput] = struct(
    string.optional[ExtraErrorOperationInput]("in", _.in),
  ){
    ExtraErrorOperationInput.apply
  }.withId(id).addHints(hints)
}
