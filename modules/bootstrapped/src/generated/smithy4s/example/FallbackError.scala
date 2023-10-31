package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class FallbackError(error: String) extends Smithy4sThrowable {
}

object FallbackError extends ShapeTag.Companion[FallbackError] {
  val id: ShapeId = ShapeId("smithy4s.example", "FallbackError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[FallbackError] = struct(
    string.required[FallbackError]("error", _.error),
  ){
    FallbackError.apply
  }.withId(id).addHints(hints)
}
