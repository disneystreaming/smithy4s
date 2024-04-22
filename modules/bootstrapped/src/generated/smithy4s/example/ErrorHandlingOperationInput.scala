package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorHandlingOperationInput(in: Option[String] = None)

object ErrorHandlingOperationInput extends ShapeTag.Companion[ErrorHandlingOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(in: Option[String]): ErrorHandlingOperationInput = ErrorHandlingOperationInput(in)

  implicit val schema: Schema[ErrorHandlingOperationInput] = struct(
    string.optional[ErrorHandlingOperationInput]("in", _.in),
  )(make).withId(id).addHints(hints)
}
