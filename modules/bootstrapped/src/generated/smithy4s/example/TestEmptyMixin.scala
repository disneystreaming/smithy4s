package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.long

final case class TestEmptyMixin(a: Option[Long] = None) extends EmptyMixin

object TestEmptyMixin extends ShapeTag.Companion[TestEmptyMixin] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestEmptyMixin")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[TestEmptyMixin] = struct(
    long.optional[TestEmptyMixin]("a", _.a),
  ){
    TestEmptyMixin.apply
  }.withId(id).addHints(hints)
}
