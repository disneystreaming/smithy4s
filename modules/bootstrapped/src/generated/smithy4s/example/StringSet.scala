package smithy4s.example

import smithy.api.UniqueItems
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.set
import smithy4s.schema.Schema.string

object StringSet extends Newtype[Set[String]] {
  val underlyingSchema: Schema[Set[String]] = set(string)
  .withId(ShapeId("smithy4s.example", "StringSet"))
  .addHints(
    UniqueItems(),
  )

  implicit val schema: Schema[StringSet] = bijection(underlyingSchema, asBijection)
}
