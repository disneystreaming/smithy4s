package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ServerErrorCustomMessage(messageField: Option[String] = None) extends Throwable {
  override def getMessage(): String = messageField.orNull
}
object ServerErrorCustomMessage extends ShapeTag.Companion[ServerErrorCustomMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ServerErrorCustomMessage")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )

  object Optics {
    val messageField = Lens[ServerErrorCustomMessage, Option[String]](_.messageField)(n => a => a.copy(messageField = n))
  }

  implicit val schema: Schema[ServerErrorCustomMessage] = struct(
    string.optional[ServerErrorCustomMessage]("messageField", _.messageField),
  ){
    ServerErrorCustomMessage.apply
  }.withId(id).addHints(hints)
}
