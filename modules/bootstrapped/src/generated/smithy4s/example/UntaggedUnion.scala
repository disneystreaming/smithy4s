package smithy4s.example

import alloy.Untagged
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
object UntaggedUnion extends ShapeTag.Companion[UntaggedUnion] {
  final case class ThreeCase(three: Three) extends UntaggedUnion { final def _ordinal: Int = 0 }
  def three(three:Three): UntaggedUnion = ThreeCase(three)
  final case class FourCase(four: Four) extends UntaggedUnion { final def _ordinal: Int = 1 }
  def four(four:Four): UntaggedUnion = FourCase(four)

  object ThreeCase {
    val schema: Schema[ThreeCase] = bijection(Three.schema
    .addHints(
      Hints.empty
    )
    , ThreeCase(_), _.three)
    val alt = schema.oneOf[UntaggedUnion]("three")
  }
  object FourCase {
    val schema: Schema[FourCase] = bijection(Four.schema
    .addHints(
      Hints.empty
    )
    , FourCase(_), _.four)
    val alt = schema.oneOf[UntaggedUnion]("four")
  }

  implicit val schema: Schema[UntaggedUnion] = union(
    ThreeCase.alt,
    FourCase.alt,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "UntaggedUnion"))
  .addHints(
    Hints(
      Untagged(),
    )
  )
}
