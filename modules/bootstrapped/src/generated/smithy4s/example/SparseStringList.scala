package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object SparseStringList extends Newtype[List[Option[String]]] {
  val id: ShapeId = ShapeId("smithy4s.example", "SparseStringList")
  val hints: Hints = Hints(
    smithy.api.Sparse(),
  )
  val underlyingSchema: Schema[List[Option[String]]] = list(string.option).withId(id).addHints(hints)
  implicit val schema: Schema[SparseStringList] = bijection(underlyingSchema, asBijection)
}
