package smithy4s.example

import smithy4s.Bijection
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait Food extends scala.Product with scala.Serializable {
  @inline final def widen: Food = this
  def _ordinal: Int
}
object Food extends ShapeTag.Companion[Food] {
  final case class PizzaCase(pizza: Pizza) extends Food { final def _ordinal: Int = 0 }
  final case class SaladCase(salad: Salad) extends Food { final def _ordinal: Int = 1 }

  object PizzaCase {
    implicit val fromValue: Bijection[Pizza, PizzaCase] = Bijection(PizzaCase(_), _.pizza)
    implicit val toValue: Bijection[PizzaCase, Pizza] = fromValue.swap
    val schema: Schema[PizzaCase] = bijection(Pizza.schema, fromValue)
  }
  object SaladCase {
    implicit val fromValue: Bijection[Salad, SaladCase] = Bijection(SaladCase(_), _.salad)
    implicit val toValue: Bijection[SaladCase, Salad] = fromValue.swap
    val schema: Schema[SaladCase] = bijection(Salad.schema, fromValue)
  }

  val pizza = PizzaCase.schema.oneOf[Food]("pizza")
  val salad = SaladCase.schema.oneOf[Food]("salad")

  implicit val schema: Schema[Food] = union(
    pizza,
    salad,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "Food"))
}
