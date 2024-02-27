package smithy4s.example

import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.struct

final case class ErrorNullableCustomTypeMessage(message: Option[Nullable[CustomErrorMessageType]] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.flatMap(_.toOption).map(_.value).orNull
}

object ErrorNullableCustomTypeMessage extends ShapeTag.Companion[ErrorNullableCustomTypeMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorNullableCustomTypeMessage")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  ).lazily

  implicit val schema: Schema[ErrorNullableCustomTypeMessage] = struct(
    CustomErrorMessageType.schema.nullable.optional[ErrorNullableCustomTypeMessage]("message", _.message),
  ){
    ErrorNullableCustomTypeMessage.apply
  }.withId(id).addHints(hints)
}
