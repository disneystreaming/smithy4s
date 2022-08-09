package smithy4s.example

import smithy4s.schema.Schema._

case class TestEmptyMixin(a: Option[Long] = None) extends EmptyMixin
object TestEmptyMixin extends smithy4s.ShapeTag.Companion[TestEmptyMixin] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "TestEmptyMixin")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[TestEmptyMixin] = struct(
    long.optional[TestEmptyMixin]("a", _.a),
  ){
    TestEmptyMixin.apply
  }.withId(id).addHints(hints)
}