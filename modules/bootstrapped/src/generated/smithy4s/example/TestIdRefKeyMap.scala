package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object TestIdRefKeyMap extends Newtype[Map[smithy4s.ShapeId, String]] {
  val id: _root_.smithy4s.ShapeId = _root_.smithy4s.ShapeId("smithy4s.example", "TestIdRefKeyMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[smithy4s.ShapeId, String]] = map(string.refined[smithy4s.ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)), string).withId(id).addHints(hints)
  implicit val schema: Schema[TestIdRefKeyMap] = bijection(underlyingSchema, asBijection)
}
