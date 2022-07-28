package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.Schema._

object TestString extends Newtype[String] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "TestString")
  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy4s.example.TestTrait(Some(smithy4s.example.OrderType.InStoreOrder(smithy4s.example.OrderNumber(100), Some("someLocation")))),
  )
  val underlyingSchema : smithy4s.Schema[String] = string.withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[TestString] = bijection(underlyingSchema, asBijection)
}