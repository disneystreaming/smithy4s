package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class KeyNotFoundError(message: String) extends Throwable {
  override def getMessage(): String = message
}
object KeyNotFoundError extends ShapeTag.Companion[KeyNotFoundError] {
  val id: ShapeId = ShapeId("smithy4s.example", "KeyNotFoundError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[KeyNotFoundError] = struct(
    string.required[KeyNotFoundError]("message", _.message),
  ){
    KeyNotFoundError.apply
  }.withId(id).addHints(hints)
}
