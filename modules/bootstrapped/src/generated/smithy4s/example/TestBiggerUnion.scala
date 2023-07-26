package smithy4s.example

import alloy.Discriminated
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait TestBiggerUnion extends scala.Product with scala.Serializable {
  @inline final def widen: TestBiggerUnion = this
  def _ordinal: Int
}
object TestBiggerUnion extends ShapeTag.Companion[TestBiggerUnion] {
  final case class OneCase(one: One) extends TestBiggerUnion { final def _ordinal: Int = 0 }
  def one(one:One): TestBiggerUnion = OneCase(one)
  final case class TwoCase(two: Two) extends TestBiggerUnion { final def _ordinal: Int = 1 }
  def two(two:Two): TestBiggerUnion = TwoCase(two)

  object OneCase {
    val schema: Schema[OneCase] = bijection(One.schema
    .addHints(
      Hints.empty
    )
    , OneCase(_), _.one)
    val alt = schema.oneOf[TestBiggerUnion]("one")
  }
  object TwoCase {
    val schema: Schema[TwoCase] = bijection(Two.schema
    .addHints(
      Hints.empty
    )
    , TwoCase(_), _.two)
    val alt = schema.oneOf[TestBiggerUnion]("two")
  }

  implicit val schema: Schema[TestBiggerUnion] = union(
    OneCase.alt,
    TwoCase.alt,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "TestBiggerUnion"))
  .addHints(
    Hints(
      Discriminated("tpe"),
    )
  )
}
