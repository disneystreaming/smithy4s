package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class UnauthorizedError(reason: String) extends Smithy4sThrowable {
}

object UnauthorizedError extends ShapeTag.Companion[UnauthorizedError] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnauthorizedError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[UnauthorizedError] = struct(
    string.required[UnauthorizedError]("reason", _.reason),
  ){
    UnauthorizedError.apply
  }.withId(id).addHints(hints)
}
