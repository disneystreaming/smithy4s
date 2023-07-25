package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Salad(name: String, ingredients: List[Ingredient])
object Salad extends ShapeTag.Companion[Salad] {
  val hints: Hints = Hints.empty

  val name = string.required[Salad]("name", _.name).addHints(smithy.api.Required())
  val ingredients = Ingredients.underlyingSchema.required[Salad]("ingredients", _.ingredients).addHints(smithy.api.Required())

  implicit val schema: Schema[Salad] = struct(
    name,
    ingredients,
  ){
    Salad.apply
  }.withId(ShapeId("smithy4s.example", "Salad")).addHints(hints)
}
