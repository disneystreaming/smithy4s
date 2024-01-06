package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object TestIdRefList extends Newtype[List[smithy4s.ShapeId]] {
  val id: _root_.smithy4s.ShapeId = _root_.smithy4s.ShapeId("smithy4s.example", "TestIdRefList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[smithy4s.ShapeId]] = list(string.refined[smithy4s.ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None))).withId(id).addHints(hints)
  implicit val schema: Schema[TestIdRefList] = bijection(underlyingSchema, asBijection)
}
