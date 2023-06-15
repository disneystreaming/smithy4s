package smithy.test

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object NonEmptyStringList extends Newtype[List[NonEmptyString]] {
  val id: ShapeId = ShapeId("smithy.test", "NonEmptyStringList")
  val hints: Hints = Hints(
    smithy.api.Private(),
  )
  val underlyingSchema: Schema[List[NonEmptyString]] = list(NonEmptyString.schema).withId(id).addHints(hints)
  implicit val schema: Schema[NonEmptyStringList] = bijection(underlyingSchema, asBijection)
}
