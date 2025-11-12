package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.union

sealed trait ConsList extends scala.Product with scala.Serializable { self =>
  @inline final def widen: ConsList = this
  def $ordinal: Int

  object project {
    def cons: Option[Cons] = ConsList.ConsCase.alt.project.lift(self).map(_.cons)
    def nil: Option[Nil] = ConsList.NilCase.alt.project.lift(self).map(_.nil)
  }

  def accept[A](visitor: ConsList.Visitor[A]): A = this match {
    case value: ConsList.ConsCase => visitor.cons(value.cons)
    case value: ConsList.NilCase => visitor.nil(value.nil)
  }
}
object ConsList extends ShapeTag.Companion[ConsList] {

  def cons(cons: Cons): ConsList = ConsCase(cons)
  def nil(nil: Nil): ConsList = NilCase(nil)

  val id: ShapeId = ShapeId("smithy4s.example", "ConsList")

  val hints: Hints = Hints.empty

  final case class ConsCase(cons: Cons) extends ConsList { final def $ordinal: Int = 0 }
  final case class NilCase(nil: Nil) extends ConsList { final def $ordinal: Int = 1 }

  object ConsCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ConsList.ConsCase] = bijection(Cons.schema.addHints(hints), ConsList.ConsCase(_), _.cons)
    val alt = schema.oneOf[ConsList]("cons")
  }
  object NilCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ConsList.NilCase] = bijection(Nil.schema.addHints(hints), ConsList.NilCase(_), _.nil)
    val alt = schema.oneOf[ConsList]("nil")
  }

  trait Visitor[A] {
    def cons(value: Cons): A
    def nil(value: Nil): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def cons(value: Cons): A = default
      def nil(value: Nil): A = default
    }
  }

  implicit val schema: Schema[ConsList] = recursive(union(
    ConsList.ConsCase.alt,
    ConsList.NilCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints))
}
