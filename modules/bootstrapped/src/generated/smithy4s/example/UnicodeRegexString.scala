package smithy4s.example

import smithy.api.Pattern
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object UnicodeRegexString extends Newtype[String] {
  val underlyingSchema: Schema[String] = string
  .withId(ShapeId("smithy4s.example", "UnicodeRegexString"))
  .addHints(
    Hints.empty
  )
  .validated(Pattern("^\uD83D\uDE0E$"))
  implicit val schema: Schema[UnicodeRegexString] = bijection(underlyingSchema, asBijection)
}
