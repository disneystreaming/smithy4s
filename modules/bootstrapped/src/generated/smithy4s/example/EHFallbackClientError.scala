package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHFallbackClientError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object EHFallbackClientError extends ShapeTag.Companion[EHFallbackClientError] {
  val id: ShapeId = ShapeId("smithy4s.example", "EHFallbackClientError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  object Lenses {
    val message = Lens[EHFallbackClientError, Option[String]](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[EHFallbackClientError] = struct(
    string.optional[EHFallbackClientError]("message", _.message),
  ){
    EHFallbackClientError.apply
  }.withId(id).addHints(hints)
}
