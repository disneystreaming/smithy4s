package smithy4s.example

import smithy4s.schema.Schema._

case class TestItOut(age: Option[smithy4s.example.refined.Age] = None, personAge: Option[smithy4s.example.refined.Age] = None)
object TestItOut extends smithy4s.ShapeTag.Companion[TestItOut] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "TestItOut")

  val hints : smithy4s.Hints = smithy4s.Hints.empty

  implicit val schema: smithy4s.Schema[TestItOut] = struct(
    int.refined(smithy4s.example.AgeFormat())(smithy4s.example.refined.Age.provider).optional[TestItOut]("age", _.age),
    int.refined(smithy4s.example.AgeFormat())(smithy4s.example.refined.Age.provider).optional[TestItOut]("personAge", _.personAge),
  ){
    TestItOut.apply
  }.withId(id).addHints(hints)
}