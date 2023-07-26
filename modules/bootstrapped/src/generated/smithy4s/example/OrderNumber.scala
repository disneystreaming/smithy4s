package smithy4s.example

import smithy.api.Box
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object OrderNumber extends Newtype[Int] {
  val underlyingSchema: Schema[Int] = int
  .withId(ShapeId("smithy4s.example", "OrderNumber"))
  .addHints(
    Box(),
  )

  implicit val schema: Schema[OrderNumber] = bijection(underlyingSchema, asBijection)
}
