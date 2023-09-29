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

  implicit val schema: Schema[Pizza] = struct(
    string.required[Pizza]("name", _.name),
    PizzaBase.schema.required[Pizza]("base", _.base),
    Ingredients.underlyingSchema.required[Pizza]("toppings", _.toppings),
  ){
    Pizza.apply
  }.withId(id).addHints(hints)
}
