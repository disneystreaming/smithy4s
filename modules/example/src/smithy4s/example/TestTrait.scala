package smithy4s.example

import smithy4s.schema.Schema._

case class TestTrait(orderType: Option[OrderType] = None)
object TestTrait extends smithy4s.ShapeTag.Companion[TestTrait] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "TestTrait")

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Trait(None, None, None),
  )

  implicit val schema: smithy4s.Schema[TestTrait] = struct(
    OrderType.schema.optional[TestTrait]("orderType", _.orderType),
  ){
    TestTrait.apply
  }.withId(id).addHints(hints)
}