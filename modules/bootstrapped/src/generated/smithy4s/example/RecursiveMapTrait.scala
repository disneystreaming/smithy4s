package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string

object RecursiveMapTrait extends Newtype[Map[String, String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveMapTrait")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy.api", "trait"), smithy4s.Document.obj()),
  ).lazily
  val underlyingSchema: Schema[Map[String, String]] = map(string.addMemberHints(Hints.Binding.DynamicBinding(ShapeId("smithy4s.example", "RecursiveMapTrait"), smithy4s.Document.obj())), string).withId(id).addHints(hints)
  implicit val schema: Schema[RecursiveMapTrait] = recursive(bijection(underlyingSchema, asBijection))
}
