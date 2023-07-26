package smithy4s.example

import smithy.api.Error
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnauthorizedError(reason: String) extends Throwable {
}
object UnauthorizedError extends ShapeTag.Companion[UnauthorizedError] {

  val reason: FieldLens[UnauthorizedError, String] = string.required[UnauthorizedError]("reason", _.reason, n => c => c.copy(reason = n)).addHints(Required())

  implicit val schema: Schema[UnauthorizedError] = struct(
    reason,
  ){
    UnauthorizedError.apply
  }
  .withId(ShapeId("smithy4s.example", "UnauthorizedError"))
  .addHints(
    Error.CLIENT.widen,
  )
}
