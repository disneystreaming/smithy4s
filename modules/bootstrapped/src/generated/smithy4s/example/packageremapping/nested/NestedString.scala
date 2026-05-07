package smithy4s.example.packageremapping.nested

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object NestedString extends Newtype[String] {
  val id: ShapeId = ShapeId("pkg.remapping.nested", "NestedString")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[NestedString] = bijection(underlyingSchema, asBijection)
}
