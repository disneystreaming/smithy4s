package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Pizza(name: String, base: PizzaBase, toppings: List[Ingredient])
object Pizza extends ShapeTag.Companion[Pizza] {

  val name = string.required[Pizza]("name", _.name, n => c => c.copy(name = n)).addHints(Required())
  val base = PizzaBase.schema.required[Pizza]("base", _.base, n => c => c.copy(base = n)).addHints(Required())
  val toppings = Ingredients.underlyingSchema.required[Pizza]("toppings", _.toppings, n => c => c.copy(toppings = n)).addHints(Required())

  implicit val schema: Schema[Pizza] = struct(
    name,
    base,
    toppings,
  ){
    Pizza.apply
  }
  .withId(ShapeId("smithy4s.example", "Pizza"))
  .addHints(
    Hints.empty
  )
}
