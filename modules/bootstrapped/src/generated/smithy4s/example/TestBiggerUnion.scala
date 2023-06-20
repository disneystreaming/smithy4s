package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait TestBiggerUnion extends scala.Product with scala.Serializable {
  @inline final def widen: TestBiggerUnion = this
}
object TestBiggerUnion extends ShapeTag.Companion[TestBiggerUnion] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestBiggerUnion")

  val hints: Hints = Hints(
    alloy.Discriminated("tpe"),
  )

  final case class OneCase(one: One) extends TestBiggerUnion
  final case class TwoCase(two: Two) extends TestBiggerUnion

  object OneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[OneCase] = bijection(One.schema.addHints(hints), OneCase(_), _.one)
    val alt = schema.oneOf[TestBiggerUnion]("one")
  }
  object TwoCase {
    val hints: Hints = Hints.empty
    val schema: Schema[TwoCase] = bijection(Two.schema.addHints(hints), TwoCase(_), _.two)
    val alt = schema.oneOf[TestBiggerUnion]("two")
  }

  implicit val schema: Schema[TestBiggerUnion] = union(
    OneCase.alt,
    TwoCase.alt,
  ){
    case c: OneCase => OneCase.alt(c)
    case c: TwoCase => TwoCase.alt(c)
  }.withId(id).addHints(hints)
}
