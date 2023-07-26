package smithy4s.example

import smithy.api.Box
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object OrderNumber extends Newtype[Int] {
  val id: ShapeId = ShapeId("smithy4s.example", "OrderNumber")
  val hints: Hints = Hints(
    Box(),
  )
  val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema: Schema[OrderNumber] = bijection(underlyingSchema, asBijection)
}
