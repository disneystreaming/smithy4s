package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SpecificServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object SpecificServerError extends ShapeTag.Companion[SpecificServerError] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "SpecificServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(599),
  )

  object Lenses {
    val message = Lens[SpecificServerError, Option[String]](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[SpecificServerError] = struct(
    string.optional[SpecificServerError]("message", _.message),
  ){
    SpecificServerError.apply
  }.withId(id).addHints(hints)
}
