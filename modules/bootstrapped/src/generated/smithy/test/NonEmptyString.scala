package smithy.test

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object NonEmptyString extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy.test", "NonEmptyString")
  val hints: Hints = Hints(
    smithy.api.Private(),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None))
  implicit val schema: Schema[NonEmptyString] = bijection(underlyingSchema, asBijection)
}
