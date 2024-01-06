package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.bijection
import _root_.smithy4s.schema.Schema.union

sealed trait UntaggedUnion extends _root_.scala.Product with _root_.scala.Serializable { self =>
  @inline final def widen: UntaggedUnion = this
  def $ordinal: Int

  object project {
    def three: Option[Three] = UntaggedUnion.ThreeCase.alt.project.lift(self).map(_.three)
    def four: Option[Four] = UntaggedUnion.FourCase.alt.project.lift(self).map(_.four)
  }

  def accept[A](visitor: UntaggedUnion.Visitor[A]): A = this match {
    case value: UntaggedUnion.ThreeCase => visitor.three(value.three)
    case value: UntaggedUnion.FourCase => visitor.four(value.four)
  }
}
object UntaggedUnion extends ShapeTag.Companion[UntaggedUnion] {

  def three(three: Three): UntaggedUnion = ThreeCase(three)
  def four(four: Four): UntaggedUnion = FourCase(four)

  val id: ShapeId = ShapeId("smithy4s.example", "UntaggedUnion")

  val hints: Hints = Hints(
    alloy.Untagged(),
  )

  final case class ThreeCase(three: Three) extends UntaggedUnion { final def $ordinal: Int = 0 }
  final case class FourCase(four: Four) extends UntaggedUnion { final def $ordinal: Int = 1 }

  object ThreeCase {
    val hints: Hints = Hints.empty
    val schema: Schema[UntaggedUnion.ThreeCase] = bijection(Three.schema.addHints(hints), UntaggedUnion.ThreeCase(_), _.three)
    val alt = schema.oneOf[UntaggedUnion]("three")
  }
  object FourCase {
    val hints: Hints = Hints.empty
    val schema: Schema[UntaggedUnion.FourCase] = bijection(Four.schema.addHints(hints), UntaggedUnion.FourCase(_), _.four)
    val alt = schema.oneOf[UntaggedUnion]("four")
  }

  trait Visitor[A] {
    def three(value: Three): A
    def four(value: Four): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def three(value: Three): A = default
      def four(value: Four): A = default
    }
  }

  implicit val schema: Schema[UntaggedUnion] = union(
    UntaggedUnion.ThreeCase.alt,
    UntaggedUnion.FourCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
