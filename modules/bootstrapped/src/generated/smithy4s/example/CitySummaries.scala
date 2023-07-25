package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object CitySummaries extends Newtype[List[CitySummary]] {
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[CitySummary]] = list(CitySummary.schema).withId(ShapeId("smithy4s.example", "CitySummaries")).addHints(hints)
  implicit val schema: Schema[CitySummaries] = bijection(underlyingSchema, asBijection)
}
