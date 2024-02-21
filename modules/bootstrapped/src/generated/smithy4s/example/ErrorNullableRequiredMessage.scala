package smithy4s.example

import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorNullableRequiredMessage(message: Nullable[String]) extends Smithy4sThrowable {
  override def getMessage(): String = message.toOption.orNull
}

object ErrorNullableRequiredMessage extends ShapeTag.Companion[ErrorNullableRequiredMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorNullableRequiredMessage")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  ).lazily

  implicit val schema: Schema[ErrorNullableRequiredMessage] = struct(
    string.nullable.required[ErrorNullableRequiredMessage]("message", _.message),
  ){
    ErrorNullableRequiredMessage.apply
  }.withId(id).addHints(hints)
}
