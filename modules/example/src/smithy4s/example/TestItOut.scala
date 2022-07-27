package smithy4s.example

import smithy4s.schema.Schema._

case class TestItOut(age: Option[smithy4s.example.refined.Age] = None, personAge: Option[smithy4s.example.refined.Age] = None, fancyList: Option[smithy4s.example.refined.FancyList] = None, name: Option[smithy4s.example.refined.Name] = None)
object TestItOut extends smithy4s.ShapeTag.Companion[TestItOut] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "TestItOut")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[TestItOut] = struct(
    Age.underlyingSchema.optional[TestItOut]("age", _.age),
    PersonAge.underlyingSchema.optional[TestItOut]("personAge", _.personAge),
    FancyList.underlyingSchema.optional[TestItOut]("fancyList", _.fancyList),
    Name.underlyingSchema.optional[TestItOut]("name", _.name),
  ){
    TestItOut.apply
  }.withId(id).addHints(hints)
}