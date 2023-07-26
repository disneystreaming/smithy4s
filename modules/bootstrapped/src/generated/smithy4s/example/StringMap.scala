package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object StringMap extends Newtype[Map[String, String]] {
  val underlyingSchema: Schema[Map[String, String]] = map(string, string)
  .withId(ShapeId("smithy4s.example", "StringMap"))
  .addHints(
    Hints.empty
  )

  implicit val schema: Schema[StringMap] = bijection(underlyingSchema, asBijection)
}
