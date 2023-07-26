package smithy4s.example

import smithy.api.Error
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class KeyNotFoundError(message: String) extends Throwable {
  override def getMessage(): String = message
}
object KeyNotFoundError extends ShapeTag.$Companion[KeyNotFoundError] {
  val $id: ShapeId = ShapeId("smithy4s.example", "KeyNotFoundError")

  val $hints: Hints = Hints(
    Error.CLIENT.widen,
  )

  val message: FieldLens[KeyNotFoundError, String] = string.required[KeyNotFoundError]("message", _.message, n => c => c.copy(message = n)).addHints(Required())

  implicit val $schema: Schema[KeyNotFoundError] = struct(
    message,
  ){
    KeyNotFoundError.apply
  }.withId($id).addHints($hints)
}
