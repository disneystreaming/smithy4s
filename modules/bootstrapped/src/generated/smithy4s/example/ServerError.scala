package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ServerError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object ServerError extends ShapeTag.Companion[ServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "ServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  ).lazily

  // constructor using the original order from the spec
  private def make(message: Option[String]): ServerError = ServerError(message)

  implicit val schema: Schema[ServerError] = struct(
    string.optional[ServerError]("message", _.message),
  ){
    make
  }.withId(id).addHints(hints)
}
