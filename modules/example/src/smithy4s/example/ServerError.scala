package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

case class ServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object ServerError extends ShapeTag.Companion[ServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "ServerError")

  val hints : Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )

  implicit val schema: Schema[ServerError] = struct(
    string.optional[ServerError]("message", _.message),
  ){
    ServerError.apply
  }.withId(id).addHints(hints)
}