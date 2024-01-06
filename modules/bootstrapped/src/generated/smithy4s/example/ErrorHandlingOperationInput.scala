package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class ErrorHandlingOperationInput(in: Option[String] = None)

object ErrorHandlingOperationInput extends ShapeTag.Companion[ErrorHandlingOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[ErrorHandlingOperationInput] = struct(
    string.optional[ErrorHandlingOperationInput]("in", _.in),
  ){
    ErrorHandlingOperationInput.apply
  }.withId(id).addHints(hints)
}
