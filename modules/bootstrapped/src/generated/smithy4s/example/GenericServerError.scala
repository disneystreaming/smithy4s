package smithy4s.example

import smithy.api.Error
import smithy.api.HttpError
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GenericServerError(message: String) extends Throwable {
  override def getMessage(): String = message
}
object GenericServerError extends ShapeTag.$Companion[GenericServerError] {
  val $id: ShapeId = ShapeId("smithy4s.example", "GenericServerError")

  val $hints: Hints = Hints(
    Error.SERVER.widen,
    HttpError(502),
  )

  val message: FieldLens[GenericServerError, String] = string.required[GenericServerError]("message", _.message, n => c => c.copy(message = n)).addHints(Required())

  implicit val $schema: Schema[GenericServerError] = struct(
    message,
  ){
    GenericServerError.apply
  }.withId($id).addHints($hints)
}
