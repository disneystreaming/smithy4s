package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

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
