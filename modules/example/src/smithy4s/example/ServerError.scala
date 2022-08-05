package smithy4s.example

import smithy4s.schema.Schema._

case class ServerError(message: Option[String]=None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object ServerError extends smithy4s.ShapeTag.Companion[ServerError] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ServerError")
  
  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Error.SERVER.widen,
  )
  
  implicit val schema: smithy4s.Schema[ServerError] = struct(
    string.optional[ServerError]("message", _.message),
  ){
    ServerError.apply
  }.withId(id).addHints(hints)
}