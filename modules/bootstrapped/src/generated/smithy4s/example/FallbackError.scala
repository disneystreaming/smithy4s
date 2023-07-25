package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class FallbackError(error: String) extends Throwable {
}
object FallbackError extends ShapeTag.Companion[FallbackError] {
  val id: ShapeId = ShapeId("smithy4s.example", "FallbackError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  val error = string.required[FallbackError]("error", _.error).addHints(smithy.api.Required())

  implicit val schema: Schema[FallbackError] = struct(
    error,
  ){
    FallbackError.apply
  }.withId(id).addHints(hints)
}
