package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.set
import smithy4s.schema.Schema.string

object TestIdRefSet extends Newtype[Set[smithy4s.ShapeId]] {
  val id: _root_.smithy4s.ShapeId = _root_.smithy4s.ShapeId("smithy4s.example", "TestIdRefSet")
  val hints: Hints = Hints(
    smithy.api.UniqueItems(),
  )
  val underlyingSchema: Schema[Set[smithy4s.ShapeId]] = set(string.refined[smithy4s.ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None))).withId(id).addHints(hints)
  implicit val schema: Schema[TestIdRefSet] = bijection(underlyingSchema, asBijection)
}
