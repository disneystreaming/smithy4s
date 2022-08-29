package smithy4s.example

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object MyMap extends Newtype[Map[Key,Value]] {
  val id: ShapeId = ShapeId("smithy4s.example", "myMap")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[Map[Key,Value]] = map(Key.schema, Value.schema).withId(id).addHints(hints)
  implicit val schema : Schema[MyMap] = bijection(underlyingSchema, asBijection)
}