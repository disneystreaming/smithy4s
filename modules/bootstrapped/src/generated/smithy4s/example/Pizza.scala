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

  // constructor using the original order from the spec
  private def make(name: String, base: PizzaBase, toppings: List[Ingredient]): Pizza = Pizza(name, base, toppings)

  implicit val schema: Schema[Pizza] = struct(
    string.required[Pizza]("name", _.name),
    PizzaBase.schema.required[Pizza]("base", _.base),
    Ingredients.underlyingSchema.required[Pizza]("toppings", _.toppings),
  ){
    make
  }.withId(id).addHints(hints)
}
