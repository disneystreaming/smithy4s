package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
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

  val message = string.optional[GenericServerError]("message", _.message)

  implicit val schema: Schema[GenericServerError] = struct(
    message,
  ){
    GenericServerError.apply
  }.withId(id).addHints(hints)
}
