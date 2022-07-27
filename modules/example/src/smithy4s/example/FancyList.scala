package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.Schema._

object FancyList extends Newtype[smithy4s.example.refined.FancyList] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "FancyList")
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  val underlyingSchema : smithy4s.Schema[smithy4s.example.refined.FancyList] = list(string).refined(smithy4s.example.FancyListFormat())(smithy4s.example.refined.FancyList.provider).withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[FancyList] = bijection(underlyingSchema, asBijection)
}