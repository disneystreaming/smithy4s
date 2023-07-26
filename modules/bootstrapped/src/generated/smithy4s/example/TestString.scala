package smithy4s.example

import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.example.OrderType.InStoreOrder
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object TestString extends Newtype[String] {
  val underlyingSchema: Schema[String] = string
  .withId(ShapeId("smithy4s.example", "TestString"))
  .addHints(
    TestTrait(orderType = Some(InStoreOrder(id = OrderNumber(100), locationId = Some("someLocation")))),
  )

  implicit val schema: Schema[TestString] = bijection(underlyingSchema, asBijection)
}
