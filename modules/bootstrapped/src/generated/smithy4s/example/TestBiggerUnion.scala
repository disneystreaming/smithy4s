package smithy4s.example

import alloy.Discriminated
import smithy4s.Bijection
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
  final case class TwoCase(two: Two) extends TestBiggerUnion { final def _ordinal: Int = 1 }

  object OneCase {
    implicit val fromValue: Bijection[One, OneCase] = Bijection(OneCase(_), _.one)
    implicit val toValue: Bijection[OneCase, One] = fromValue.swap
    val schema: Schema[OneCase] = bijection(One.schema, fromValue)
  }
  object TwoCase {
    implicit val fromValue: Bijection[Two, TwoCase] = Bijection(TwoCase(_), _.two)
    implicit val toValue: Bijection[TwoCase, Two] = fromValue.swap
    val schema: Schema[TwoCase] = bijection(Two.schema, fromValue)
  }

  val one = OneCase.schema.oneOf[TestBiggerUnion]("one")
  val two = TwoCase.schema.oneOf[TestBiggerUnion]("two")

  implicit val schema: Schema[TestBiggerUnion] = union(
    one,
    two,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "TestBiggerUnion"))
  .addHints(
    Discriminated("tpe"),
  )
}
