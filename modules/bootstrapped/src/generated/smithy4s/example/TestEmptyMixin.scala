package smithy4s.example

import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.long
import smithy4s.schema.Schema.struct

final case class TestEmptyMixin(a: Option[Long] = None) extends EmptyMixin
object TestEmptyMixin extends ShapeTag.Companion[TestEmptyMixin] {

  val a: FieldLens[TestEmptyMixin, Option[Long]] = long.optional[TestEmptyMixin]("a", _.a, n => c => c.copy(a = n))

  implicit val schema: Schema[TestEmptyMixin] = struct(
    a,
  ){
    TestEmptyMixin.apply
  }
  .withId(ShapeId("smithy4s.example", "TestEmptyMixin"))
}
