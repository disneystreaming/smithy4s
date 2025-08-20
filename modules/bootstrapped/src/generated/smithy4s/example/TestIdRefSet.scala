package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.set
import smithy4s.schema.Schema.string

object TestIdRefSet extends Newtype[Set[ShapeId]] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestIdRefSet")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy.api", "uniqueItems"), smithy4s.Document.obj()),
  ).lazily
  val underlyingSchema: Schema[Set[ShapeId]] = set(string.refined[ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)).addMemberHints(Hints.Binding.DynamicBinding(ShapeId("smithy.api", "idRef"), smithy4s.Document.obj()))).withId(id).addHints(hints)
  implicit val schema: Schema[TestIdRefSet] = bijection(underlyingSchema, asBijection)
}
