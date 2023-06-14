package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import main.refined.NonEmptyList
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object NonEmptyCandies extends Newtype[NonEmptyList[Candy]] {
  val id: ShapeId = ShapeId("smithy4s.example", "NonEmptyCandies")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[NonEmptyList[Candy]] = list(Candy.schema).refined[NonEmptyList[Candy]](smithy4s.example.NonEmptyListFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[NonEmptyCandies] = bijection(underlyingSchema, asBijection)
}
