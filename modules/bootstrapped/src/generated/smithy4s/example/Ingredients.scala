package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object Ingredients extends Newtype[List[Ingredient]] {
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Ingredient]] = list(Ingredient.schema).withId(ShapeId("smithy4s.example", "Ingredients")).addHints(hints)
  implicit val schema: Schema[Ingredients] = bijection(underlyingSchema, asBijection)
}
