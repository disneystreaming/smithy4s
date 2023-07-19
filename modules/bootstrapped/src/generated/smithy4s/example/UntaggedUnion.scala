package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Prism
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait UntaggedUnion extends scala.Product with scala.Serializable {
  @inline final def widen: UntaggedUnion = this
}
object UntaggedUnion extends ShapeTag.Companion[UntaggedUnion] {
  val id: ShapeId = ShapeId("smithy4s.example", "UntaggedUnion")

  val hints: Hints = Hints(
    alloy.Untagged(),
  )

  object Optics {
    val three = Prism.partial[UntaggedUnion, Three]{ case ThreeCase(t) => t }(ThreeCase.apply)
    val four = Prism.partial[UntaggedUnion, Four]{ case FourCase(t) => t }(FourCase.apply)
  }

  final case class ThreeCase(three: Three) extends UntaggedUnion
  final case class FourCase(four: Four) extends UntaggedUnion

  object ThreeCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ThreeCase] = bijection(Three.schema.addHints(hints), ThreeCase(_), _.three)
    val alt = schema.oneOf[UntaggedUnion]("three")
  }
  object FourCase {
    val hints: Hints = Hints.empty
    val schema: Schema[FourCase] = bijection(Four.schema.addHints(hints), FourCase(_), _.four)
    val alt = schema.oneOf[UntaggedUnion]("four")
  }

  implicit val schema: Schema[UntaggedUnion] = union(
    ThreeCase.alt,
    FourCase.alt,
  ){
    case c: ThreeCase => ThreeCase.alt(c)
    case c: FourCase => FourCase.alt(c)
  }.withId(id).addHints(hints)
}
