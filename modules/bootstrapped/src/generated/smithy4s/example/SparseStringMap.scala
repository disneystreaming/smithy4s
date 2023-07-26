package smithy4s.example

import smithy.api.Sparse
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object SparseStringMap extends Newtype[Map[String, Option[String]]] {
  val underlyingSchema: Schema[Map[String, Option[String]]] = map(string, string.option)
  .withId(ShapeId("smithy4s.example", "SparseStringMap"))
  .addHints(
    Hints(
      Sparse(),
    )
  )

  implicit val schema: Schema[SparseStringMap] = bijection(underlyingSchema, asBijection)
}
