package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GenericServerError(message: String) extends Throwable {
  override def getMessage(): String = message
}
object GenericServerError extends ShapeTag.Companion[GenericServerError] {
  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(502),
  )

  val message = string.required[GenericServerError]("message", _.message).addHints(smithy.api.Required())

  implicit val schema: Schema[GenericServerError] = struct(
    message,
  ){
    GenericServerError.apply
  }.withId(ShapeId("smithy4s.example", "GenericServerError")).addHints(hints)
}
