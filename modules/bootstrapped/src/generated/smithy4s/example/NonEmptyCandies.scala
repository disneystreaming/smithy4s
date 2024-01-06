package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.refined.NonEmptyList
import smithy4s.schema.Schema.list

object NonEmptyCandies extends Newtype[NonEmptyList[Candy]] {
  val id: ShapeId = ShapeId("smithy4s.example", "NonEmptyCandies")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[NonEmptyList[Candy]] = list(Candy.schema).refined[NonEmptyList[Candy]](smithy4s.example.NonEmptyListFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[NonEmptyCandies] = bijection(underlyingSchema, asBijection)
}
