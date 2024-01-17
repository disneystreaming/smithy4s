package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherClientError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object RandomOtherClientError extends ShapeTag.Companion[RandomOtherClientError] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherClientError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  ).lazily

  implicit val schema: Schema[RandomOtherClientError] = struct(
    string.optional[RandomOtherClientError]("message", _.message),
  ){
    RandomOtherClientError.apply
  }.withId(id).addHints(hints)
}
