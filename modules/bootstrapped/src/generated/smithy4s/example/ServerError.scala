package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class ServerError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object ServerError extends ShapeTag.Companion[ServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "ServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  )

  implicit val schema: Schema[ServerError] = struct(
    string.optional[ServerError]("message", _.message),
  ){
    ServerError.apply
  }.withId(id).addHints(hints)
}
