package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object TestString extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestString")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example", "testTrait"), smithy4s.Document.obj("orderType" -> smithy4s.Document.obj("inStore" -> smithy4s.Document.obj("id" -> smithy4s.Document.fromDouble(100.0d), "locationId" -> smithy4s.Document.fromString("someLocation"))))),
  ).lazily
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[TestString] = bijection(underlyingSchema, asBijection)
}
