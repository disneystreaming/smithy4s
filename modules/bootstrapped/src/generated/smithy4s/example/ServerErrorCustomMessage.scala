package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ServerErrorCustomMessage(messageField: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = messageField.orNull
}

object ServerErrorCustomMessage extends ShapeTag.Companion[ServerErrorCustomMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ServerErrorCustomMessage")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  ).lazily

  // constructor using the original order from the spec
  private def make(messageField: Option[String]): ServerErrorCustomMessage = ServerErrorCustomMessage(messageField)

  implicit val schema: Schema[ServerErrorCustomMessage] = struct(
    string.optional[ServerErrorCustomMessage]("messageField", _.messageField),
  )(make).withId(id).addHints(hints)
}
