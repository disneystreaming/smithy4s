package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object TestString extends Newtype[String] {
  val hints: Hints = Hints(
    smithy4s.example.TestTrait(orderType = Some(smithy4s.example.OrderType.InStoreOrder(id = smithy4s.example.OrderNumber(100), locationId = Some("someLocation")))),
  )
  val underlyingSchema: Schema[String] = string.withId(ShapeId("smithy4s.example", "TestString")).addHints(hints)
  implicit val schema: Schema[TestString] = bijection(underlyingSchema, asBijection)
}
