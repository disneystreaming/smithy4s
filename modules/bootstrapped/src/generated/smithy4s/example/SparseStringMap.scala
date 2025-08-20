package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Nullable
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object SparseStringMap extends Newtype[Map[String, Nullable[String]]] {
  val id: ShapeId = ShapeId("smithy4s.example", "SparseStringMap")
  val hints: Hints = Hints(
    smithy.api.Sparse(),
  ).lazily
  val underlyingSchema: Schema[Map[String, Nullable[String]]] = map(string, string.nullable).withId(id).addHints(hints)
  implicit val schema: Schema[SparseStringMap] = bijection(underlyingSchema, asBijection)
}
