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

  // constructor using the original order from the spec
  private def make(message: Option[String]): RandomOtherClientError = RandomOtherClientError(message)

  implicit val schema: Schema[RandomOtherClientError] = struct(
    string.optional[RandomOtherClientError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
