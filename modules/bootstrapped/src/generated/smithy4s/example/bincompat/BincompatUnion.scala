package smithy4s.example.bincompat

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait BincompatUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: BincompatUnion = this
  def $ordinal: Int

  object project {
    def s1: Option[BincompatEmptyStruct] = BincompatUnion.S1Case.alt.project.lift(self).map(_.s1)
    def s2: Option[BincompatEmptyStruct] = BincompatUnion.S2Case.alt.project.lift(self).map(_.s2)
  }

  def accept[A](visitor: BincompatUnion.Visitor[A]): A = this match {
    case value: BincompatUnion.S1Case => visitor.s1(value.s1)
    case value: BincompatUnion.S2Case => visitor.s2(value.s2)
  }
}
object BincompatUnion extends ShapeTag.Companion[BincompatUnion] {

  def s1(s1: BincompatEmptyStruct): BincompatUnion = S1Case(s1)
  def s2(s2: BincompatEmptyStruct): BincompatUnion = S2Case(s2)

  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatUnion")

  val hints: Hints = Hints.empty

  final case class S1Case(s1: BincompatEmptyStruct) extends BincompatUnion { final def $ordinal: Int = 0 }
  final case class S2Case(s2: BincompatEmptyStruct) extends BincompatUnion { final def $ordinal: Int = 1 }

  object S1Case {
    val hints: Hints = Hints.empty
    val schema: Schema[BincompatUnion.S1Case] = bijection(BincompatEmptyStruct.schema.addHints(hints), BincompatUnion.S1Case(_), _.s1)
    val alt = schema.oneOf[BincompatUnion]("s1")
    def unapply(self: BincompatUnion): Option[BincompatUnion.S1Case] = self match { case BincompatUnion.S1Case(value) => Some(value); case _ => None }
  }
  object S2Case {
    val hints: Hints = Hints.empty
    val schema: Schema[BincompatUnion.S2Case] = bijection(BincompatEmptyStruct.schema.addHints(hints), BincompatUnion.S2Case(_), _.s2)
    val alt = schema.oneOf[BincompatUnion]("s2")
    def unapply(self: BincompatUnion): Option[BincompatUnion.S2Case] = self match { case BincompatUnion.S2Case(value) => Some(value); case _ => None }
  }

  sealed trait Visitor[A] {
    def s1(value: BincompatEmptyStruct): A
    def s2(value: BincompatEmptyStruct): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def s1(value: BincompatEmptyStruct): A = default
      def s2(value: BincompatEmptyStruct): A = default
    }
  }

  implicit val schema: Schema[BincompatUnion] = union(
    BincompatUnion.S1Case.alt,
    BincompatUnion.S2Case.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
