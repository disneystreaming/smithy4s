package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHFallbackServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object EHFallbackServerError extends ShapeTag.Companion[EHFallbackServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "EHFallbackServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )

  object Optics {
    val message = Lens[EHFallbackServerError, Option[String]](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[EHFallbackServerError] = struct(
    string.optional[EHFallbackServerError]("message", _.message),
  ){
    EHFallbackServerError.apply
  }.withId(id).addHints(hints)
}
