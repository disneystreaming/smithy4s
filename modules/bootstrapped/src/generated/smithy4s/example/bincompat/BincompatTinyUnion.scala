package smithy4s.example.bincompat

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait BincompatTinyUnion extends scala.Product with scala.Serializable { self =>
  @inline final def widen: BincompatTinyUnion = this
  def $ordinal: Int

  object project {
    def s1: Option[BincompatEmptyStruct] = BincompatTinyUnion.S1Case.alt.project.lift(self).map(_.s1)
  }

  def accept[A](visitor: BincompatTinyUnion.Visitor[A]): A = this match {
    case value: BincompatTinyUnion.S1Case => visitor.s1(value.s1)
  }
}
object BincompatTinyUnion extends ShapeTag.Companion[BincompatTinyUnion] {

  def s1(s1: BincompatEmptyStruct): BincompatTinyUnion = S1Case(s1)

  val id: ShapeId = ShapeId("smithy4s.example.bincompat", "BincompatTinyUnion")

  val hints: Hints = Hints.empty

  final case class S1Case(s1: BincompatEmptyStruct) extends BincompatTinyUnion { final def $ordinal: Int = 0 }

  object S1Case {
    val hints: Hints = Hints.empty
    val schema: Schema[BincompatTinyUnion.S1Case] = bijection(BincompatEmptyStruct.schema.addHints(hints), BincompatTinyUnion.S1Case(_), _.s1)
    val alt = schema.oneOf[BincompatTinyUnion]("s1")
    def unapply(self: BincompatTinyUnion): Option[BincompatTinyUnion.S1Case] = self match { case BincompatTinyUnion.S1Case(value) => Some(value); case _ => None }
  }

  sealed trait Visitor[A] {
    def s1(value: BincompatEmptyStruct): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def s1(value: BincompatEmptyStruct): A = default
    }
  }

  implicit val schema: Schema[BincompatTinyUnion] = union(
    BincompatTinyUnion.S1Case.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
