package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object ReservedNameUnionTrait extends Newtype[java.lang.String] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "ReservedNameUnionTrait")
  val hints: Hints = Hints(
    smithy4s.example.collision.class.AdtStruct(),
  ).lazily
  val underlyingSchema: Schema[java.lang.String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[ReservedNameUnionTrait] = bijection(underlyingSchema, asBijection)
}
