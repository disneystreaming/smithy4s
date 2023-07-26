package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map

object MyMap extends Newtype[Map[String, String]] {
  val underlyingSchema: Schema[Map[String, String]] = map(String.schema, String.schema)
  .withId(ShapeId("smithy4s.example.collision", "MyMap"))
  .addHints(
    Hints.empty
  )

  implicit val schema: Schema[MyMap] = bijection(underlyingSchema, asBijection)
}
