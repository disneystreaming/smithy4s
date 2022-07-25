package smithy4s.example

import smithy4s.schema.Schema._

case class TestItOut(age: Option[Age] = None, personAge: Option[PersonAge] = None)
object TestItOut extends smithy4s.ShapeTag.Companion[TestItOut] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "TestItOut")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[TestItOut] = struct(
    Age.schema.optional[TestItOut]("age", _.age),
    PersonAge.schema.optional[TestItOut]("personAge", _.personAge),
  ){
    TestItOut.apply
  }.withId(id).addHints(hints)
}
