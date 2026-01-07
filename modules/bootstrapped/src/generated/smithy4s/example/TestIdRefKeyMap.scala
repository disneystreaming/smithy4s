package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object TestIdRefKeyMap extends Newtype[Map[ShapeId, String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestIdRefKeyMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[ShapeId, String]] = map(string.refined[ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)), string).withId(id).addHints(hints)
  implicit val schema: Schema[TestIdRefKeyMap] = bijection(underlyingSchema, asBijection)
}
