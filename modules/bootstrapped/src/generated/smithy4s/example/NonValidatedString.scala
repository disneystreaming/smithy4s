package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object NonValidatedString extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "NonValidatedString")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None)).validated(smithy.api.Pattern("[a-zA-Z0-9]+"))
  implicit val schema: Schema[NonValidatedString] = bijection(underlyingSchema, asBijection)
}
