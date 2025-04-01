package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object RecursiveList extends Newtype[List[RecursiveListWrapper]] {
  val id: ShapeId = ShapeId("smithy4s.example", "RecursiveList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[RecursiveListWrapper]] = list(RecursiveListWrapper.schema).withId(id).addHints(hints)
  implicit val schema: Schema[RecursiveList] = bijection(underlyingSchema, asBijection)
}
