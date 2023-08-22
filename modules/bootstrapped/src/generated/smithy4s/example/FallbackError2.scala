package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class FallbackError2(error: String) extends Throwable {
}
object FallbackError2 extends ShapeTag.Companion[FallbackError2] {
  val id: ShapeId = ShapeId("smithy4s.example", "FallbackError2")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[FallbackError2] = struct(
    string.required[FallbackError2]("error", _.error).addHints(smithy.api.Required()),
  ){
    FallbackError2.apply
  }.withId(id).addHints(hints)
}
