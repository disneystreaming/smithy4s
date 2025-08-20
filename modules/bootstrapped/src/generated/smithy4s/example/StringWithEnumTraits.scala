package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object StringWithEnumTraits extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "StringWithEnumTraits")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example", "leftRight"), smithy4s.Document.fromString("left")),
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example", "oldStyleLeftRight"), smithy4s.Document.fromString("right")),
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example", "oneTwo"), smithy4s.Document.fromDouble(1.0d)),
  ).lazily
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[StringWithEnumTraits] = bijection(underlyingSchema, asBijection)
}
