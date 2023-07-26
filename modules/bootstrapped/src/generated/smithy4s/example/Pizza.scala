package smithy4s.example

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Pizza(name: String, base: PizzaBase, toppings: List[Ingredient])
object Pizza extends ShapeTag.Companion[Pizza] {

  val name: FieldLens[Pizza, String] = string.required[Pizza]("name", _.name, n => c => c.copy(name = n)).addHints(Required())
  val base: FieldLens[Pizza, PizzaBase] = PizzaBase.schema.required[Pizza]("base", _.base, n => c => c.copy(base = n)).addHints(Required())
  val toppings: FieldLens[Pizza, List[Ingredient]] = Ingredients.underlyingSchema.required[Pizza]("toppings", _.toppings, n => c => c.copy(toppings = n)).addHints(Required())

  implicit val schema: Schema[Pizza] = struct(
    name,
    base,
    toppings,
  ){
    Pizza.apply
  }
  .withId(ShapeId("smithy4s.example", "Pizza"))
}
