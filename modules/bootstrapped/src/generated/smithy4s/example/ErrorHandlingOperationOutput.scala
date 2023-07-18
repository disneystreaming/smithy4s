package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorHandlingOperationOutput(out: Option[String] = None)
object ErrorHandlingOperationOutput extends ShapeTag.Companion[ErrorHandlingOperationOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingOperationOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  object Lenses {
    val out = Lens[ErrorHandlingOperationOutput, Option[String]](_.out)(n => a => a.copy(out = n))
  }

  implicit val schema: Schema[ErrorHandlingOperationOutput] = struct(
    string.optional[ErrorHandlingOperationOutput]("out", _.out),
  ){
    ErrorHandlingOperationOutput.apply
  }.withId(id).addHints(hints)
}
