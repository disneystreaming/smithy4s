package smithy4s.example

import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.refined.NonEmptyList
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object NonEmptyCandies extends Newtype[NonEmptyList[Candy]] {
  val underlyingSchema: Schema[NonEmptyList[Candy]] = list(Candy.schema).refined[NonEmptyList[Candy]](NonEmptyListFormat())
  .withId(ShapeId("smithy4s.example", "NonEmptyCandies"))

  implicit val schema: Schema[NonEmptyCandies] = bijection(underlyingSchema, asBijection)
}
