package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object TestIdRefValueMap extends Newtype[Map[String, smithy4s.ShapeId]] {
  val id: _root_.smithy4s.ShapeId = _root_.smithy4s.ShapeId("smithy4s.example", "TestIdRefValueMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, smithy4s.ShapeId]] = map(string, string.refined[smithy4s.ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None))).withId(id).addHints(hints)
  implicit val schema: Schema[TestIdRefValueMap] = bijection(underlyingSchema, asBijection)
}
