package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Salad(name: String, ingredients: List[Ingredient])
object Salad extends ShapeTag.Companion[Salad] {

  val name = string.required[Salad]("name", _.name, n => c => c.copy(name = n)).addHints(Required())
  val ingredients = Ingredients.underlyingSchema.required[Salad]("ingredients", _.ingredients, n => c => c.copy(ingredients = n)).addHints(Required())

  implicit val schema: Schema[Salad] = struct(
    name,
    ingredients,
  ){
    Salad.apply
  }
  .withId(ShapeId("smithy4s.example", "Salad"))
  .addHints(
    Hints.empty
  )
}
