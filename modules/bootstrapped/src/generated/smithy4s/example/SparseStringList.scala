package smithy4s.example

import smithy.api.Sparse
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object SparseStringList extends Newtype[List[Option[String]]] {
  val underlyingSchema: Schema[List[Option[String]]] = list(string.option)
  .withId(ShapeId("smithy4s.example", "SparseStringList"))
  .addHints(
    Sparse(),
  )

  implicit val schema: Schema[SparseStringList] = bijection(underlyingSchema, asBijection)
}
