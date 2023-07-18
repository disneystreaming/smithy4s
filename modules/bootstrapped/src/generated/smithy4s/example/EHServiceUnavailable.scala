package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHServiceUnavailable(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object EHServiceUnavailable extends ShapeTag.Companion[EHServiceUnavailable] {
  val id: ShapeId = ShapeId("smithy4s.example", "EHServiceUnavailable")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(503),
  )

  object Lenses {
    val message = Lens[EHServiceUnavailable, Option[String]](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[EHServiceUnavailable] = struct(
    string.optional[EHServiceUnavailable]("message", _.message),
  ){
    EHServiceUnavailable.apply
  }.withId(id).addHints(hints)
}
