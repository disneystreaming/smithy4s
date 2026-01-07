package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object MyIntList extends Newtype[List[MyInt]] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "MyIntList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[MyInt]] = list(MyInt.schema).withId(id).addHints(hints)
  implicit val schema: Schema[MyIntList] = bijection(underlyingSchema, asBijection)
}
