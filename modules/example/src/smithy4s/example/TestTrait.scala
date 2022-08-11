package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class TestTrait(orderType: Option[OrderType]=None)
object TestTrait extends ShapeTag.Companion[TestTrait] {
  val id: ShapeId = ShapeId("smithy4s.example", "testTrait")
  
  val hints : Hints = Hints(
    smithy.api.Trait(None, None, None, None),
  )
  
  implicit val schema: Schema[TestTrait] = struct(
    OrderType.schema.optional[TestTrait]("orderType", _.orderType),
  ){
    TestTrait.apply
  }.withId(id).addHints(hints)
}