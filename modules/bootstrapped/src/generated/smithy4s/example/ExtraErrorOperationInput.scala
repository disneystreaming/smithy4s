package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

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
