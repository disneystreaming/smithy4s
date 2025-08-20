package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.string

object RecursiveListTrait extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveListTrait")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy.api", "trait"), smithy4s.Document.obj()),
  ).lazily
  val underlyingSchema: Schema[List[String]] = list(string.addMemberHints(Hints.Binding.DynamicBinding(ShapeId("smithy4s.example", "RecursiveListTrait"), smithy4s.Document.array()))).withId(id).addHints(hints)
  implicit val schema: Schema[RecursiveListTrait] = recursive(bijection(underlyingSchema, asBijection))
}
