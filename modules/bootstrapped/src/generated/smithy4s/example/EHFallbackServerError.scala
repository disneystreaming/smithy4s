package smithy4s.example

import smithy.api.Error
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHFallbackServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object EHFallbackServerError extends ShapeTag.Companion[EHFallbackServerError] {

  val message = string.optional[EHFallbackServerError]("message", _.message, n => c => c.copy(message = n))

  implicit val schema: Schema[EHFallbackServerError] = struct(
    message,
  ){
    EHFallbackServerError.apply
  }
  .withId(ShapeId("smithy4s.example", "EHFallbackServerError"))
  .addHints(
    Hints(
      Error.SERVER.widen,
    )
  )
}
