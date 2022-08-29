package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.set
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object MySet extends Newtype[Set[Value]] {
  val id: ShapeId = ShapeId("smithy4s.example", "mySet")
  val hints : Hints = Hints(
    smithy.api.UniqueItems(),
  )
  val underlyingSchema : Schema[Set[Value]] = set(Value.schema).withId(id).addHints(hints)
  implicit val schema : Schema[MySet] = bijection(underlyingSchema, asBijection)
}