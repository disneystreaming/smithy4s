package smithy4s.example.collision

import smithy.api.UniqueItems
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.set

object MySet extends Newtype[Set[String]] {
  val underlyingSchema: Schema[Set[String]] = set(String.schema)
  .withId(ShapeId("smithy4s.example.collision", "MySet"))
  .addHints(
    Hints(
      UniqueItems(),
    )
  )

  implicit val schema: Schema[MySet] = bijection(underlyingSchema, asBijection)
}
