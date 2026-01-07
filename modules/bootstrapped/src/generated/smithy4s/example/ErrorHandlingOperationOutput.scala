package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorHandlingOperationOutput(out: Option[String] = None)

object ErrorHandlingOperationOutput extends ShapeTag.Companion[ErrorHandlingOperationOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingOperationOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(out: Option[String]): ErrorHandlingOperationOutput = ErrorHandlingOperationOutput(out)

  implicit val schema: Schema[ErrorHandlingOperationOutput] = struct(
    string.optional[ErrorHandlingOperationOutput]("out", _.out),
  )(make).withId(id).addHints(hints)
}
