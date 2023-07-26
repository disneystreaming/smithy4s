package smithy4s.example

import smithy.api.Input
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorHandlingOperationInput(in: Option[String] = None)
object ErrorHandlingOperationInput extends ShapeTag.$Companion[ErrorHandlingOperationInput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingOperationInput")

  val $hints: Hints = Hints(
    Input(),
  )

  val in: FieldLens[ErrorHandlingOperationInput, Option[String]] = string.optional[ErrorHandlingOperationInput]("in", _.in, n => c => c.copy(in = n))

  implicit val $schema: Schema[ErrorHandlingOperationInput] = struct(
    in,
  ){
    ErrorHandlingOperationInput.apply
  }.withId($id).addHints($hints)
}
