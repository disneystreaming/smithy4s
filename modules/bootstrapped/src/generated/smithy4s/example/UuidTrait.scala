package smithy4s.example

import java.util.UUID
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.uuid

object UuidTrait extends Newtype[UUID] {
  val id: ShapeId = ShapeId("smithy4s.example", "uuidTrait")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("alloy", "uuidFormat"), smithy4s.Document.obj()),
    Hints.Binding.DynamicBinding(ShapeId("smithy.api", "trait"), smithy4s.Document.obj()),
  ).lazily
  val underlyingSchema: Schema[UUID] = uuid.withId(id).addHints(hints)
  implicit val schema: Schema[UuidTrait] = recursive(bijection(underlyingSchema, asBijection))
}
