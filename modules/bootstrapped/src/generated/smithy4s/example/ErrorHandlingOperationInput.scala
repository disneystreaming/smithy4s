package smithy4s.example

import smithy.api.Input
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorHandlingOperationInput(in: Option[String] = None)
object ErrorHandlingOperationInput extends ShapeTag.Companion[ErrorHandlingOperationInput] {

  val in = string.optional[ErrorHandlingOperationInput]("in", _.in, n => c => c.copy(in = n))

  implicit val schema: Schema[ErrorHandlingOperationInput] = struct(
    in,
  ){
    ErrorHandlingOperationInput.apply
  }
  .withId(ShapeId("smithy4s.example", "ErrorHandlingOperationInput"))
  .addHints(
    Hints(
      Input(),
    )
  )
}
