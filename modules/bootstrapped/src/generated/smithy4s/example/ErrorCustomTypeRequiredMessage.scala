package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.struct

final case class ErrorCustomTypeRequiredMessage(message: CustomErrorMessageType) extends Smithy4sThrowable {
  override def getMessage(): String = message.value
}

object ErrorCustomTypeRequiredMessage extends ShapeTag.Companion[ErrorCustomTypeRequiredMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorCustomTypeRequiredMessage")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  ).lazily

  implicit val schema: Schema[ErrorCustomTypeRequiredMessage] = struct(
    CustomErrorMessageType.schema.required[ErrorCustomTypeRequiredMessage]("message", _.message),
  ){
    ErrorCustomTypeRequiredMessage.apply
  }.withId(id).addHints(hints)
}
