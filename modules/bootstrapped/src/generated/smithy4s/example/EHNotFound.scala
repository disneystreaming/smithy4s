package smithy4s.example

import smithy.api.Error
import smithy.api.HttpError
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHNotFound(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object EHNotFound extends ShapeTag.Companion[EHNotFound] {

  val message = string.optional[EHNotFound]("message", _.message, n => c => c.copy(message = n))

  implicit val schema: Schema[EHNotFound] = struct(
    message,
  ){
    EHNotFound.apply
  }
  .withId(ShapeId("smithy4s.example", "EHNotFound"))
  .addHints(
    Hints(
      Error.CLIENT.widen,
      HttpError(404),
    )
  )
}
