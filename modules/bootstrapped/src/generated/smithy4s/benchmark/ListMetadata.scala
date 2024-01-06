package smithy4s.benchmark

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object ListMetadata extends Newtype[List[Metadata]] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "ListMetadata")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Metadata]] = list(Metadata.schema).withId(id).addHints(hints)
  implicit val schema: Schema[ListMetadata] = bijection(underlyingSchema, asBijection)
}
