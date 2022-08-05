package smithy4s.example

import smithy4s.schema.Schema._

case class ServerErrorCustomMessage(messageField: Option[String]=None) extends Throwable {
  override def getMessage(): String = messageField.orNull
}
object ServerErrorCustomMessage extends smithy4s.ShapeTag.Companion[ServerErrorCustomMessage] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ServerErrorCustomMessage")
  
  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Error.SERVER.widen,
  )
  
  implicit val schema: smithy4s.Schema[ServerErrorCustomMessage] = struct(
    string.optional[ServerErrorCustomMessage]("messageField", _.messageField).addHints(),
  ){
    ServerErrorCustomMessage.apply
  }.withId(id).addHints(hints)
}