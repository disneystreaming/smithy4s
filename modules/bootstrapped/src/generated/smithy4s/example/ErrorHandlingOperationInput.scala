package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorHandlingOperationInput(in: Option[String] = None)
object ErrorHandlingOperationInput extends ShapeTag.Companion[ErrorHandlingOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorHandlingOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  object Lenses {
    val in = Lens[ErrorHandlingOperationInput, Option[String]](_.in)(n => a => a.copy(in = n))
  }

  implicit val schema: Schema[ErrorHandlingOperationInput] = struct(
    string.optional[ErrorHandlingOperationInput]("in", _.in),
  ){
    ErrorHandlingOperationInput.apply
  }.withId(id).addHints(hints)
}
