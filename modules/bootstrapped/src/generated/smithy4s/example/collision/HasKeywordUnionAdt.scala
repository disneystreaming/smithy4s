package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant
import smithy4s.schema.Schema.union

sealed trait HasKeywordUnionAdt extends scala.Product with scala.Serializable { self =>
  @inline final def widen: HasKeywordUnionAdt = this
  def $ordinal: Int

  object project {
    def one: Option[HasKeywordUnionAdt.Implicit] = HasKeywordUnionAdt.Implicit.alt.project.lift(self)
  }

  def accept[A](visitor: HasKeywordUnionAdt.Visitor[A]): A = this match {
    case value: HasKeywordUnionAdt.Implicit => visitor.one(value)
  }
}
object HasKeywordUnionAdt extends ShapeTag.Companion[HasKeywordUnionAdt] {

  def one(): Implicit = Implicit()

  val id: ShapeId = ShapeId("smithy4s.example.collision", "HasKeywordUnionAdt")

  val hints: Hints = Hints.empty

  final case class Implicit() extends HasKeywordUnionAdt {
    def $ordinal: Int = 0
  }

  object Implicit extends ShapeTag.Companion[Implicit] {
    val id: ShapeId = ShapeId("smithy4s.example.collision", "implicit")

    val hints: Hints = Hints.empty

    implicit val schema: Schema[Implicit] = constant(Implicit()).withId(id).addHints(hints)

    val alt = schema.oneOf[HasKeywordUnionAdt]("one")
  }


  trait Visitor[A] {
    def one(value: HasKeywordUnionAdt.Implicit): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def one(value: HasKeywordUnionAdt.Implicit): A = default
    }
  }

  implicit val schema: Schema[HasKeywordUnionAdt] = union(
    HasKeywordUnionAdt.Implicit.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
