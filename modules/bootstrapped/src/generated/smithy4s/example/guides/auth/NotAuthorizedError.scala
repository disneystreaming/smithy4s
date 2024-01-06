package smithy4s.example.guides.auth

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class NotAuthorizedError(message: String) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object NotAuthorizedError extends ShapeTag.Companion[NotAuthorizedError] {
  val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "NotAuthorizedError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(401),
  )

  implicit val schema: Schema[NotAuthorizedError] = struct(
    string.required[NotAuthorizedError]("message", _.message),
  ){
    NotAuthorizedError.apply
  }.withId(id).addHints(hints)
}
