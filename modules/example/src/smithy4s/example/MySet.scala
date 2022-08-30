package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.set
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object MySet extends Newtype[Set[StringValue]] {
  val id: ShapeId = ShapeId("smithy4s.example", "MySet")
  val hints : Hints = Hints(
    smithy.api.UniqueItems(),
  )
  val underlyingSchema : Schema[Set[StringValue]] = set(StringValue.schema).withId(id).addHints(hints)
  implicit val schema : Schema[MySet] = bijection(underlyingSchema, asBijection)
}