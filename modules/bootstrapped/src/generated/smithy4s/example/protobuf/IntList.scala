package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.list

object IntList extends Newtype[List[Int]] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "IntList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Int]] = list(int).withId(id).addHints(hints)
  implicit val schema: Schema[IntList] = bijection(underlyingSchema, asBijection)
}
