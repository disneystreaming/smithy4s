package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.bijection
import _root_.smithy4s.schema.Schema.union

sealed trait Food extends _root_.scala.Product with _root_.scala.Serializable { self =>
  @inline final def widen: Food = this
  def $ordinal: Int

  object project {
    def pizza: Option[Pizza] = Food.PizzaCase.alt.project.lift(self).map(_.pizza)
    def salad: Option[Salad] = Food.SaladCase.alt.project.lift(self).map(_.salad)
  }

  def accept[A](visitor: Food.Visitor[A]): A = this match {
    case value: Food.PizzaCase => visitor.pizza(value.pizza)
    case value: Food.SaladCase => visitor.salad(value.salad)
  }
}
object Food extends ShapeTag.Companion[Food] {

  def pizza(pizza: Pizza): Food = PizzaCase(pizza)
  def salad(salad: Salad): Food = SaladCase(salad)

  val id: ShapeId = ShapeId("smithy4s.example", "Food")

  val hints: Hints = Hints.empty

  final case class PizzaCase(pizza: Pizza) extends Food { final def $ordinal: Int = 0 }
  final case class SaladCase(salad: Salad) extends Food { final def $ordinal: Int = 1 }

  object PizzaCase {
    val hints: Hints = Hints.empty
    val schema: Schema[Food.PizzaCase] = bijection(Pizza.schema.addHints(hints), Food.PizzaCase(_), _.pizza)
    val alt = schema.oneOf[Food]("pizza")
  }
  object SaladCase {
    val hints: Hints = Hints.empty
    val schema: Schema[Food.SaladCase] = bijection(Salad.schema.addHints(hints), Food.SaladCase(_), _.salad)
    val alt = schema.oneOf[Food]("salad")
  }

  trait Visitor[A] {
    def pizza(value: Pizza): A
    def salad(value: Salad): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def pizza(value: Pizza): A = default
      def salad(value: Salad): A = default
    }
  }

  implicit val schema: Schema[Food] = union(
    Food.PizzaCase.alt,
    Food.SaladCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
