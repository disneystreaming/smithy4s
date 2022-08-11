package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class ServerErrorCustomMessage(messageField: Option[String]=None) extends Throwable {
  override def getMessage(): String = messageField.orNull
}
object ServerErrorCustomMessage extends ShapeTag.Companion[ServerErrorCustomMessage] {
  val id: ShapeId = ShapeId("smithy4s.example", "ServerErrorCustomMessage")
  
  val hints : Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )
  
  implicit val schema: Schema[ServerErrorCustomMessage] = struct(
    string.optional[ServerErrorCustomMessage]("messageField", _.messageField).addHints(),
  ){
    ServerErrorCustomMessage.apply
  }.withId(id).addHints(hints)
}