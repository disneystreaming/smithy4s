package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object CityId extends Newtype[String] {
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(ShapeId("smithy4s.example", "CityId")).addHints(hints).validated(smithy.api.Pattern("^[A-Za-z0-9 ]+$"))
  implicit val schema: Schema[CityId] = bijection(underlyingSchema, asBijection)
}
