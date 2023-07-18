package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GenericServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object GenericServerError extends ShapeTag.Companion[GenericServerError] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "GenericServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(500),
  )

  object Lenses {
    val message = Lens[GenericServerError, Option[String]](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[GenericServerError] = struct(
    string.optional[GenericServerError]("message", _.message),
  ){
    GenericServerError.apply
  }.withId(id).addHints(hints)
}
