package smithy4s.example.guides.auth

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NotAuthorizedError(message: String) extends Throwable {
  override def getMessage(): String = message
}
object NotAuthorizedError extends ShapeTag.Companion[NotAuthorizedError] {
  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(401),
  )

  val message = string.required[NotAuthorizedError]("message", _.message).addHints(smithy.api.Required())

  implicit val schema: Schema[NotAuthorizedError] = struct(
    message,
  ){
    NotAuthorizedError.apply
  }.withId(ShapeId("smithy4s.example.guides.auth", "NotAuthorizedError")).addHints(hints)
}
