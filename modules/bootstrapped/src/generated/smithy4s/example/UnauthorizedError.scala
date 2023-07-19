package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnauthorizedError(reason: String) extends Throwable {
}
object UnauthorizedError extends ShapeTag.Companion[UnauthorizedError] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnauthorizedError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  object Optics {
    val reason = Lens[UnauthorizedError, String](_.reason)(n => a => a.copy(reason = n))
  }

  implicit val schema: Schema[UnauthorizedError] = struct(
    string.required[UnauthorizedError]("reason", _.reason).addHints(smithy.api.Required()),
  ){
    UnauthorizedError.apply
  }.withId(id).addHints(hints)
}
