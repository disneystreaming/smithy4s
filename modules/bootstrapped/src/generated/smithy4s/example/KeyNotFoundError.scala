package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class KeyNotFoundError(message: String) extends Smithy4sThrowable {
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
