package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class ErrorHandlingOperationOutput(out: Option[String] = None)

object ErrorHandlingOperationOutput extends ShapeTag.Companion[ErrorHandlingOperationOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingOperationOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  implicit val schema: Schema[ErrorHandlingOperationOutput] = struct(
    string.optional[ErrorHandlingOperationOutput]("out", _.out),
  ){
    ErrorHandlingOperationOutput.apply
  }.withId(id).addHints(hints)
}
