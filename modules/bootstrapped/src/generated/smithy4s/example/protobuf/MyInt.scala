package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object MyInt extends Newtype[Int] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "MyInt")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema: Schema[MyInt] = bijection(underlyingSchema, asBijection)
}
