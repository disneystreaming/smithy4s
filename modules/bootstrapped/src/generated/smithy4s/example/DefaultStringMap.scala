package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object DefaultStringMap extends Newtype[Map[String, String]] {
  val underlyingSchema: Schema[Map[String, String]] = map(string, string)
  .withId(ShapeId("smithy4s.example", "DefaultStringMap"))
  .addHints(
    Hints.empty
  )

  implicit val schema: Schema[DefaultStringMap] = bijection(underlyingSchema, asBijection)
}
