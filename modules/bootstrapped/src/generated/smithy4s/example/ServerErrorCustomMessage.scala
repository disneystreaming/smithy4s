package smithy4s.example

import smithy.api.Error
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ServerErrorCustomMessage(messageField: Option[String] = None) extends Throwable {
  override def getMessage(): String = messageField.orNull
}
object ServerErrorCustomMessage extends ShapeTag.Companion[ServerErrorCustomMessage] {

  val messageField = string.optional[ServerErrorCustomMessage]("messageField", _.messageField, n => c => c.copy(messageField = n))

  implicit val schema: Schema[ServerErrorCustomMessage] = struct(
    messageField,
  ){
    ServerErrorCustomMessage.apply
  }
  .withId(ShapeId("smithy4s.example", "ServerErrorCustomMessage"))
  .addHints(
    Hints(
      Error.SERVER.widen,
    )
  )
}
