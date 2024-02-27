package smithy4s.example

import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.struct

final case class ErrorNullableCustomTypeRequiredMessage(message: Nullable[CustomErrorMessageType]) extends Smithy4sThrowable {
  override def getMessage(): String = message.toOption.map(_.value).orNull
}

object ErrorNullableCustomTypeRequiredMessage extends ShapeTag.Companion[ErrorNullableCustomTypeRequiredMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorNullableCustomTypeRequiredMessage")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  ).lazily

  implicit val schema: Schema[ErrorNullableCustomTypeRequiredMessage] = struct(
    CustomErrorMessageType.schema.nullable.required[ErrorNullableCustomTypeRequiredMessage]("message", _.message),
  ){
    ErrorNullableCustomTypeRequiredMessage.apply
  }.withId(id).addHints(hints)
}
