package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnauthorizedError(reason: String) extends Smithy4sThrowable {
}

object UnauthorizedError extends ShapeTag.Companion[UnauthorizedError] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnauthorizedError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  ).lazily

  // constructor using the original order from the spec
  private def make(reason: String): UnauthorizedError = UnauthorizedError(reason)

  implicit val schema: Schema[UnauthorizedError] = struct(
    string.required[UnauthorizedError]("reason", _.reason),
  )(make).withId(id).addHints(hints)
}
