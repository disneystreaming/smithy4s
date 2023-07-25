package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class MyOpError() extends Throwable {
}
object MyOpError extends ShapeTag.Companion[MyOpError] {
  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[MyOpError] = constant(MyOpError()).withId(ShapeId("smithy4s.example", "MyOpError")).addHints(hints)
}
