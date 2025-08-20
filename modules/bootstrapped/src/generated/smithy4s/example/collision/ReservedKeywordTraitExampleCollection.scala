package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object ReservedKeywordTraitExampleCollection extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedKeywordTraitExampleCollection")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example.collision", "reservedKeywordStructTrait"), smithy4s.Document.obj("implicit" -> smithy4s.Document.fromString("demo"), "package" -> smithy4s.Document.obj("class" -> smithy4s.Document.fromDouble(42.0d)))),
  ).lazily
  val underlyingSchema: Schema[List[String]] = list(String.schema.addMemberHints(Hints.Binding.DynamicBinding(ShapeId("smithy4s.example.collision", "reservedKeywordStructTrait"), smithy4s.Document.obj("implicit" -> smithy4s.Document.fromString("demo"), "package" -> smithy4s.Document.obj("class" -> smithy4s.Document.fromDouble(42.0d)))))).withId(id).addHints(hints)
  implicit val schema: Schema[ReservedKeywordTraitExampleCollection] = bijection(underlyingSchema, asBijection)
}
