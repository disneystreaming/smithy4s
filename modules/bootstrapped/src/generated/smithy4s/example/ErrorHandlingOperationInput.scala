package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorHandlingOperationInput(in: Option[String] = None)
object ErrorHandlingOperationInput extends ShapeTag.Companion[ErrorHandlingOperationInput] {
  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  val in = string.optional[ErrorHandlingOperationInput]("in", _.in)

  implicit val schema: Schema[ErrorHandlingOperationInput] = struct(
    in,
  ){
    ErrorHandlingOperationInput.apply
  }.withId(ShapeId("smithy4s.example", "ErrorHandlingOperationInput")).addHints(hints)
}
