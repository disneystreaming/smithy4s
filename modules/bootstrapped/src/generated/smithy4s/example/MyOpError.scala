package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Smithy4sThrowable
import _root_.smithy4s.schema.Schema.constant

final case class MyOpError() extends Smithy4sThrowable {
}

object MyOpError extends ShapeTag.Companion[MyOpError] {
  val id: ShapeId = ShapeId("smithy4s.example", "MyOpError")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
  )

  implicit val schema: Schema[MyOpError] = constant(MyOpError()).withId(id).addHints(hints)
}
