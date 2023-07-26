package smithy4s.example

import alloy.Untagged
import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait UntaggedUnion extends scala.Product with scala.Serializable {
  @inline final def widen: UntaggedUnion = this
  def _ordinal: Int
}
object UntaggedUnion extends ShapeTag.$Companion[UntaggedUnion] {

  def three(three:Three): UntaggedUnion = ThreeCase(three)
  def four(four:Four): UntaggedUnion = FourCase(four)

  val $id: ShapeId = ShapeId("smithy4s.example", "UntaggedUnion")

  val $hints: Hints = Hints(
    Untagged(),
  )

  final case class ThreeCase(three: Three) extends UntaggedUnion { final def _ordinal: Int = 0 }
  final case class FourCase(four: Four) extends UntaggedUnion { final def _ordinal: Int = 1 }

  object ThreeCase {
    implicit val fromValue: Bijection[Three, ThreeCase] = Bijection(ThreeCase(_), _.three)
    implicit val toValue: Bijection[ThreeCase, Three] = fromValue.swap
    val $schema: Schema[ThreeCase] = bijection(Three.$schema, fromValue)
  }
  object FourCase {
    implicit val fromValue: Bijection[Four, FourCase] = Bijection(FourCase(_), _.four)
    implicit val toValue: Bijection[FourCase, Four] = fromValue.swap
    val $schema: Schema[FourCase] = bijection(Four.$schema, fromValue)
  }

  val three = ThreeCase.$schema.oneOf[UntaggedUnion]("three")
  val four = FourCase.$schema.oneOf[UntaggedUnion]("four")

  implicit val $schema: Schema[UntaggedUnion] = union(
    three,
    four,
  ){
    _._ordinal
  }.withId($id).addHints($hints)
}
