package smithy4s.example.collision

import smithy4s.Schema
import smithy4s.schema.Schema.set
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object MySet extends Newtype[Set[_String]] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "MySet")
  val hints : Hints = Hints(
    smithy.api.UniqueItems(),
  )
  val underlyingSchema : Schema[Set[_String]] = set(_String.schema).withId(id).addHints(hints)
  implicit val schema : Schema[MySet] = bijection(underlyingSchema, asBijection)
}