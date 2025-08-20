package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Nullable
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object SparseStringList extends Newtype[List[Nullable[String]]] {
  val id: ShapeId = ShapeId("smithy4s.example", "SparseStringList")
  val hints: Hints = Hints(
    smithy.api.Sparse(),
  ).lazily
  val underlyingSchema: Schema[List[Nullable[String]]] = list(string.nullable).withId(id).addHints(hints)
  implicit val schema: Schema[SparseStringList] = bijection(underlyingSchema, asBijection)
}
