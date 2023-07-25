package smithy4s.benchmark

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object ListMetadata extends Newtype[List[Metadata]] {
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Metadata]] = list(Metadata.schema).withId(ShapeId("smithy4s.benchmark", "ListMetadata")).addHints(hints)
  implicit val schema: Schema[ListMetadata] = bijection(underlyingSchema, asBijection)
}
