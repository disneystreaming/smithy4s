package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.struct

final case class TestEmptyMixin(a: Option[Long] = None) extends EmptyMixin
object TestEmptyMixin extends ShapeTag.Companion[TestEmptyMixin] {
  val hints: Hints = Hints.empty

  val a = long.optional[TestEmptyMixin]("a", _.a)

  implicit val schema: Schema[TestEmptyMixin] = struct(
    a,
  ){
    TestEmptyMixin.apply
  }.withId(ShapeId("smithy4s.example", "TestEmptyMixin")).addHints(hints)
}
