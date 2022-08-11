package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object FancyList extends Newtype[smithy4s.example.refined.FancyList] {
  val id: ShapeId = ShapeId("smithy4s.example", "FancyList")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[smithy4s.example.refined.FancyList] = list(string).refined[smithy4s.example.refined.FancyList](smithy4s.example.FancyListFormat()).withId(id).addHints(hints)
  implicit val schema : Schema[FancyList] = bijection(underlyingSchema, asBijection)
}