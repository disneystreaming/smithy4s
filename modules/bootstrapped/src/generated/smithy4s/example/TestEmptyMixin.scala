package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.struct

final case class TestEmptyMixin(a: Option[Long] = None) extends EmptyMixin

object TestEmptyMixin extends ShapeTag.Companion[TestEmptyMixin] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestEmptyMixin")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(a: Option[Long]): TestEmptyMixin = TestEmptyMixin(a)

  implicit val schema: Schema[TestEmptyMixin] = struct(
    long.optional[TestEmptyMixin]("a", _.a),
  )(make).withId(id).addHints(hints)
}
