package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

case class TestEmptyMixin(a: Option[Long]=None) extends EmptyMixin
object TestEmptyMixin extends ShapeTag.Companion[TestEmptyMixin] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestEmptyMixin")
  
  val hints : Hints = Hints.empty
  
  implicit val schema: Schema[TestEmptyMixin] = struct(
    long.optional[TestEmptyMixin]("a", _.a),
  ){
    TestEmptyMixin.apply
  }.withId(id).addHints(hints)
}