package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHFallbackServerError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object EHFallbackServerError extends ShapeTag.Companion[EHFallbackServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "EHFallbackServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )

  implicit val schema: Schema[EHFallbackServerError] = struct(
    string.optional[EHFallbackServerError]("message", _.message),
  ){
    EHFallbackServerError.apply
  }.withId(id).addHints(hints)
}
