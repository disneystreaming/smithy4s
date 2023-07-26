package smithy4s.example

import smithy.api.Trait
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

/** @param orderType
  *   Our order types have different ways to identify a product
  *   Except for preview orders, these don't have an ID
  */
final case class TestTrait(orderType: Option[OrderType] = None)
object TestTrait extends ShapeTag.Companion[TestTrait] {

  implicit val schema: Schema[TestTrait] = recursive(struct(
    orderType,
  ){
    TestTrait.apply
  }
  .withId(ShapeId("smithy4s.example", "testTrait"))
  .addHints(
    Hints(
      Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
    )
  ))

  val orderType = OrderType.schema.optional[TestTrait]("orderType", _.orderType, n => c => c.copy(orderType = n))
}
