package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHFallbackClientError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object EHFallbackClientError extends ShapeTag.Companion[EHFallbackClientError] {
  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  val message = string.optional[EHFallbackClientError]("message", _.message)

  implicit val schema: Schema[EHFallbackClientError] = struct(
    message,
  ){
    EHFallbackClientError.apply
  }.withId(ShapeId("smithy4s.example", "EHFallbackClientError")).addHints(hints)
}
