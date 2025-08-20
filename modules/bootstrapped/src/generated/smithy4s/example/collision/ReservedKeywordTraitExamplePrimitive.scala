package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object ReservedKeywordTraitExamplePrimitive extends Newtype[java.lang.String] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedKeywordTraitExamplePrimitive")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example.collision", "reservedKeywordStructTrait"), smithy4s.Document.obj("implicit" -> smithy4s.Document.fromString("demo"), "package" -> smithy4s.Document.obj("class" -> smithy4s.Document.fromDouble(42.0d)))),
  ).lazily
  val underlyingSchema: Schema[java.lang.String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[ReservedKeywordTraitExamplePrimitive] = bijection(underlyingSchema, asBijection)
}
