package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object TestIdRefList extends Newtype[List[ShapeId]] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestIdRefList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[ShapeId]] = list(string.refined[ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None))).withId(id).addHints(hints)
  implicit val schema: Schema[TestIdRefList] = bijection(underlyingSchema, asBijection)
}
