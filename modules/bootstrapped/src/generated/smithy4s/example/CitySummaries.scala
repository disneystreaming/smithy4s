package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object CitySummaries extends Newtype[List[CitySummary]] {
  val id: ShapeId = ShapeId("smithy4s.example", "CitySummaries")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[CitySummary]] = list(CitySummary.schema).withId(id).addHints(hints)
  implicit val schema: Schema[CitySummaries] = bijection(underlyingSchema, asBijection)
}
