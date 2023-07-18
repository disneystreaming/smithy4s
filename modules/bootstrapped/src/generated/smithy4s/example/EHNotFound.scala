package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class EHNotFound(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object EHNotFound extends ShapeTag.Companion[EHNotFound] {
  val id: ShapeId = ShapeId("smithy4s.example", "EHNotFound")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(404),
  )

  object Lenses {
    val message = Lens[EHNotFound, Option[String]](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[EHNotFound] = struct(
    string.optional[EHNotFound]("message", _.message),
  ){
    EHNotFound.apply
  }.withId(id).addHints(hints)
}
