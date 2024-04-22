package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NotFoundError(name: String) extends Smithy4sThrowable {
}

object NotFoundError extends ShapeTag.Companion[NotFoundError] {
  val id: ShapeId = ShapeId("smithy4s.example", "NotFoundError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(404),
  ).lazily

  // constructor using the original order from the spec
  private def make(name: String): NotFoundError = NotFoundError(name)

  implicit val schema: Schema[NotFoundError] = struct(
    string.required[NotFoundError]("name", _.name),
  )(make).withId(id).addHints(hints)
}
