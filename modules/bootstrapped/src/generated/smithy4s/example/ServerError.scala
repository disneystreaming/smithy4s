package smithy4s.example

import smithy.api.Error
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ServerError(message: Option[String] = None) extends Throwable {
  override def getMessage(): String = message.orNull
}
object ServerError extends ShapeTag.Companion[ServerError] {

  val message = string.optional[ServerError]("message", _.message, n => c => c.copy(message = n))

  implicit val schema: Schema[ServerError] = struct(
    message,
  ){
    ServerError.apply
  }
  .withId(ShapeId("smithy4s.example", "ServerError"))
  .addHints(
    Hints(
      Error.SERVER.widen,
    )
  )
}
