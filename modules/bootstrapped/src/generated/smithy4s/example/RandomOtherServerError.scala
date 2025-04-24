package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class RandomOtherServerError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object RandomOtherServerError extends ShapeTag.Companion[RandomOtherServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "RandomOtherServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
  ).lazily

  // constructor using the original order from the spec
  private def make(message: Option[String]): RandomOtherServerError = RandomOtherServerError(message)

  implicit val schema: Schema[RandomOtherServerError] = struct(
    string.optional[RandomOtherServerError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
