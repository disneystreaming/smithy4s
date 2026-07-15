package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.union

sealed trait NestedInnerUnionTarget extends scala.Product with scala.Serializable { self =>
  @inline final def widen: NestedInnerUnionTarget = this
  def $ordinal: Int

  object project {
    def str: Option[String] = NestedInnerUnionTarget.StrCase.alt.project.lift(self).map(_.str)
    def num: Option[Int] = NestedInnerUnionTarget.NumCase.alt.project.lift(self).map(_.num)
  }

  def accept[A](visitor: NestedInnerUnionTarget.Visitor[A]): A = this match {
    case value: NestedInnerUnionTarget.StrCase => visitor.str(value.str)
    case value: NestedInnerUnionTarget.NumCase => visitor.num(value.num)
  }
}
object NestedInnerUnionTarget extends ShapeTag.Companion[NestedInnerUnionTarget] {

  def str(str: String): NestedInnerUnionTarget = StrCase(str)
  def num(num: Int): NestedInnerUnionTarget = NumCase(num)

  val id: ShapeId = ShapeId("smithy4s.example", "NestedInnerUnionTarget")

  val hints: Hints = Hints.empty

  final case class StrCase(str: String) extends NestedInnerUnionTarget { final def $ordinal: Int = 0 }
  final case class NumCase(num: Int) extends NestedInnerUnionTarget { final def $ordinal: Int = 1 }

  object StrCase {
    val hints: Hints = Hints.empty
    val schema: Schema[NestedInnerUnionTarget.StrCase] = bijection(string.addHints(hints), NestedInnerUnionTarget.StrCase(_), _.str)
    val alt = schema.oneOf[NestedInnerUnionTarget]("str")
  }
  object NumCase {
    val hints: Hints = Hints.empty
    val schema: Schema[NestedInnerUnionTarget.NumCase] = bijection(int.addHints(hints), NestedInnerUnionTarget.NumCase(_), _.num)
    val alt = schema.oneOf[NestedInnerUnionTarget]("num")
  }

  trait Visitor[A] {
    def str(value: String): A
    def num(value: Int): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def str(value: String): A = default
      def num(value: Int): A = default
    }
  }

  implicit val schema: Schema[NestedInnerUnionTarget] = union[NestedInnerUnionTarget](
    NestedInnerUnionTarget.StrCase.alt,
    NestedInnerUnionTarget.NumCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
