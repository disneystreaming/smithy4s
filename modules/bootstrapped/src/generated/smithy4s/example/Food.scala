package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed abstract class Food extends scala.Product with scala.Serializable {
  @inline final def widen: Food = this
  def _ordinal: Int
}
object Food extends ShapeTag.Companion[Food] {
  val id: ShapeId = ShapeId("smithy4s.example", "Food")

  val hints: Hints = Hints.empty

  final case class PizzaCase(pizza: Pizza) extends Food { final def _ordinal: Int = 0 }
  final case class SaladCase(salad: Salad) extends Food { final def _ordinal: Int = 1 }

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
    _._ordinal
  }.withId(id).addHints(hints)
}
