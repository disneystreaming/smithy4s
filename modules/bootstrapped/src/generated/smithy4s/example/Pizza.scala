package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Pizza(name: String, base: PizzaBase, toppings: List[Ingredient])
object Pizza extends ShapeTag.Companion[Pizza] {
  val id: ShapeId = ShapeId("smithy4s.example", "Pizza")

  val hints: Hints = Hints.empty

  val name = string.required[Pizza]("name", _.name).addHints(smithy.api.Required())
  val base = PizzaBase.schema.required[Pizza]("base", _.base).addHints(smithy.api.Required())
  val toppings = Ingredients.underlyingSchema.required[Pizza]("toppings", _.toppings).addHints(smithy.api.Required())

  implicit val schema: Schema[Pizza] = struct(
    name,
    base,
    toppings,
  ){
    Pizza.apply
  }.withId(id).addHints(hints)
}
