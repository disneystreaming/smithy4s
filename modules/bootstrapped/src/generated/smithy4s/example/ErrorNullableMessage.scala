package smithy4s.example

import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ErrorNullableMessage(message: Option[Nullable[String]] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.flatMap(_.toOption).orNull
}

object ErrorNullableMessage extends ShapeTag.Companion[ErrorNullableMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorNullableMessage")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  ).lazily

  implicit val schema: Schema[ErrorNullableMessage] = struct(
    string.nullable.optional[ErrorNullableMessage]("message", _.message),
  ){
    ErrorNullableMessage.apply
  }.withId(id).addHints(hints)
}
