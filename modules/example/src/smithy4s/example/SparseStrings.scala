package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object SparseStrings extends Newtype[List[Option[String]]] {
  val id: ShapeId = ShapeId("smithy4s.example", "SparseStrings")
  val hints: Hints = Hints(
    smithy.api.Sparse(),
  )
  val underlyingSchema: Schema[List[Option[String]]] = list(string.sparse).withId(id).addHints(hints)
  implicit val schema: Schema[SparseStrings] = bijection(underlyingSchema, asBijection)
}
