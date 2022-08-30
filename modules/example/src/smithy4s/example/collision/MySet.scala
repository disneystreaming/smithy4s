package smithy4s.example.collision

import smithy4s.Schema
import smithy4s.schema.Schema.set
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object MySet extends Newtype[Set[String]] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "MySet")
  val hints : Hints = Hints(
    smithy.api.UniqueItems(),
  )
  val underlyingSchema : Schema[Set[String]] = set(_String.underlyingSchema).withId(id).addHints(hints)
  implicit val schema : Schema[MySet] = bijection(underlyingSchema, asBijection)
}