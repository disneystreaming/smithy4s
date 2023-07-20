package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Pizza(name: String, base: PizzaBase, toppings: List[Ingredient])
object Pizza extends ShapeTag.Companion[Pizza] {
  val id: ShapeId = ShapeId("smithy4s.example", "Pizza")

  val hints: Hints = Hints.empty

  object Optics {
    val nameLens = Lens[Pizza, String](_.name)(n => a => a.copy(name = n))
    val baseLens = Lens[Pizza, PizzaBase](_.base)(n => a => a.copy(base = n))
    val toppingsLens = Lens[Pizza, List[Ingredient]](_.toppings)(n => a => a.copy(toppings = n))
  }

  implicit val schema: Schema[Pizza] = struct(
    string.required[Pizza]("name", _.name).addHints(smithy.api.Required()),
    PizzaBase.schema.required[Pizza]("base", _.base).addHints(smithy.api.Required()),
    Ingredients.underlyingSchema.required[Pizza]("toppings", _.toppings).addHints(smithy.api.Required()),
  ){
    Pizza.apply
  }.withId(id).addHints(hints)
}
