package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

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
