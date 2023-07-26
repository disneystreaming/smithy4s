package smithy4s.example

import smithy.api.Error
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class MyOpError() extends Throwable {
}
object MyOpError extends ShapeTag.$Companion[MyOpError] {
  val $id: ShapeId = ShapeId("smithy4s.example", "MyOpError")

  val $hints: Hints = Hints(
    Error.CLIENT.widen,
  )

  implicit val $schema: Schema[MyOpError] = constant(MyOpError()).withId($id).addHints($hints)
}
