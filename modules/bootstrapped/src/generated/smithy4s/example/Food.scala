package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait Food extends scala.Product with scala.Serializable {
  @inline final def widen: Food = this
}
object Food extends ShapeTag.Companion[Food] {
  val id: ShapeId = ShapeId("smithy4s.example", "Food")

  val hints: Hints = Hints.empty

  object Optics {
    val pizza = Prism.partial[Food, Pizza]{ case PizzaCase(t) => t }(PizzaCase.apply)
    val salad = Prism.partial[Food, Salad]{ case SaladCase(t) => t }(SaladCase.apply)
  }

  final case class PizzaCase(pizza: Pizza) extends Food
  final case class SaladCase(salad: Salad) extends Food

  object PizzaCase {
    val hints: Hints = Hints.empty
    val schema: Schema[PizzaCase] = bijection(Pizza.schema.addHints(hints), PizzaCase(_), _.pizza)
    val alt = schema.oneOf[Food]("pizza")
  }
  object SaladCase {
    val hints: Hints = Hints.empty
    val schema: Schema[SaladCase] = bijection(Salad.schema.addHints(hints), SaladCase(_), _.salad)
    val alt = schema.oneOf[Food]("salad")
  }

  implicit val schema: Schema[Food] = union(
    PizzaCase.alt,
    SaladCase.alt,
  ){
    case c: PizzaCase => PizzaCase.alt(c)
    case c: SaladCase => SaladCase.alt(c)
  }.withId(id).addHints(hints)
}
