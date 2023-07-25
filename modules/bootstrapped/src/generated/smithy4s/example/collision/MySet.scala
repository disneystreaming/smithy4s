package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.set

object MySet extends Newtype[Set[String]] {
  val hints: Hints = Hints(
    smithy.api.UniqueItems(),
  )
  val underlyingSchema: Schema[Set[String]] = set(String.schema).withId(ShapeId("smithy4s.example.collision", "MySet")).addHints(hints)
  implicit val schema: Schema[MySet] = bijection(underlyingSchema, asBijection)
}
