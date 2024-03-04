package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class SpecificServerError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object SpecificServerError extends ShapeTag.Companion[SpecificServerError] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "SpecificServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(599),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: Option[String]): SpecificServerError = SpecificServerError(message)

  implicit val schema: Schema[SpecificServerError] = struct(
    string.optional[SpecificServerError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
