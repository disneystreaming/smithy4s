package smithy4s.example.guides.auth

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NotAuthorizedError(message: String) extends Throwable {
  override def getMessage(): String = message
}
object NotAuthorizedError extends ShapeTag.Companion[NotAuthorizedError] {
  val id: ShapeId = ShapeId("smithy4s.example.guides.auth", "NotAuthorizedError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(401),
  )

  object Optics {
    val message = Lens[NotAuthorizedError, String](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[NotAuthorizedError] = struct(
    string.required[NotAuthorizedError]("message", _.message).addHints(smithy.api.Required()),
  ){
    NotAuthorizedError.apply
  }.withId(id).addHints(hints)
}
