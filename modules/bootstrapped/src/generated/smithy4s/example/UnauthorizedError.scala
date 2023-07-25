package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnauthorizedError(reason: String) extends Throwable {
}
object UnauthorizedError extends ShapeTag.Companion[UnauthorizedError] {
  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  val reason = string.required[UnauthorizedError]("reason", _.reason).addHints(smithy.api.Required())

  implicit val schema: Schema[UnauthorizedError] = struct(
    reason,
  ){
    UnauthorizedError.apply
  }.withId(ShapeId("smithy4s.example", "UnauthorizedError")).addHints(hints)
}
