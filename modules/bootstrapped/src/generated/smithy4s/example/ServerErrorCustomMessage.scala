package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class ServerErrorCustomMessage(messageField: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = messageField.orNull
}

object ServerErrorCustomMessage extends ShapeTag.Companion[ServerErrorCustomMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ServerErrorCustomMessage")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )

  implicit val schema: Schema[ServerErrorCustomMessage] = struct(
    string.optional[ServerErrorCustomMessage]("messageField", _.messageField),
  ){
    ServerErrorCustomMessage.apply
  }.withId(id).addHints(hints)
}
