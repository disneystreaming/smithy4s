package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.recursive
import _root_.smithy4s.schema.Schema.struct

/** @param orderType
  *   Our order types have different ways to identify a product
  *   Except for preview orders, these don't have an ID 
  */
final case class TestTrait(orderType: Option[OrderType] = None)

object TestTrait extends ShapeTag.Companion[TestTrait] {
  val id: ShapeId = ShapeId("smithy4s.example", "testTrait")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[TestTrait] = recursive(struct(
    OrderType.schema.optional[TestTrait]("orderType", _.orderType),
  ){
    TestTrait.apply
  }.withId(id).addHints(hints))
}
