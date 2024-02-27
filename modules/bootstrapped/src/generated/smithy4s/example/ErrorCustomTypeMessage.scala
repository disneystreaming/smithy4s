package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.struct

final case class ErrorCustomTypeMessage(message: Option[CustomErrorMessageType] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.map(_.value).orNull
}

object ErrorCustomTypeMessage extends ShapeTag.Companion[ErrorCustomTypeMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ErrorCustomTypeMessage")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  ).lazily

  implicit val schema: Schema[ErrorCustomTypeMessage] = struct(
    CustomErrorMessageType.schema.optional[ErrorCustomTypeMessage]("message", _.message),
  ){
    ErrorCustomTypeMessage.apply
  }.withId(id).addHints(hints)
}
