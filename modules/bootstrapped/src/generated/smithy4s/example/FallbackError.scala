package smithy4s.example

import smithy.api.Error
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class FallbackError(error: String) extends Throwable {
}
object FallbackError extends ShapeTag.Companion[FallbackError] {

  val error = string.required[FallbackError]("error", _.error, n => c => c.copy(error = n)).addHints(Required())

  implicit val schema: Schema[FallbackError] = struct(
    error,
  ){
    FallbackError.apply
  }
  .withId(ShapeId("smithy4s.example", "FallbackError"))
  .addHints(
    Hints(
      Error.CLIENT.widen,
    )
  )
}
