package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorRequiredMessage(message: String) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object ErrorRequiredMessage extends ShapeTag.Companion[ErrorRequiredMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorRequiredMessage")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  ).lazily

  implicit val schema: Schema[ErrorRequiredMessage] = struct(
    string.required[ErrorRequiredMessage]("message", _.message),
  ){
    ErrorRequiredMessage.apply
  }.withId(id).addHints(hints)
}
