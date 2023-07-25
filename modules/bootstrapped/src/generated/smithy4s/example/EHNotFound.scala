package smithy4s.example

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
  val id: ShapeId = ShapeId("smithy4s.example", "EHNotFound")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(404),
  )

  val message = string.optional[EHNotFound]("message", _.message)

  implicit val schema: Schema[EHNotFound] = struct(
    message,
  ){
    EHNotFound.apply
  }.withId(id).addHints(hints)
}
