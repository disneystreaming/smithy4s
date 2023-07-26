package smithy4s.example.guides.auth

import smithy.api.Error
import smithy.api.HttpError
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NotAuthorizedError(message: String) extends Throwable {
  override def getMessage(): String = message
}
object NotAuthorizedError extends ShapeTag.Companion[NotAuthorizedError] {

  val message: FieldLens[NotAuthorizedError, String] = string.required[NotAuthorizedError]("message", _.message, n => c => c.copy(message = n)).addHints(Required())

  implicit val schema: Schema[NotAuthorizedError] = struct(
    message,
  ){
    NotAuthorizedError.apply
  }
  .withId(ShapeId("smithy4s.example.guides.auth", "NotAuthorizedError"))
  .addHints(
    Error.CLIENT.widen,
    HttpError(401),
  )
}
