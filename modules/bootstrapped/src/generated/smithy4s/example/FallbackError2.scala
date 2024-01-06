package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class FallbackError2(error: String) extends Smithy4sThrowable {
}

object FallbackError2 extends ShapeTag.Companion[FallbackError2] {
  val id: ShapeId = ShapeId("smithy4s.example", "FallbackError2")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[FallbackError2] = struct(
    string.required[FallbackError2]("error", _.error),
  ){
    FallbackError2.apply
  }.withId(id).addHints(hints)
}
