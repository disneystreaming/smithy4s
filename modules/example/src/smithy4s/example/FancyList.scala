package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string
import main.refined.FancyList

object FancyList extends Newtype[smithy4s.refined.FancyList] {
  val id: ShapeId = ShapeId("smithy4s.example", "FancyList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[smithy4s.refined.FancyList] = list(string).refined[smithy4s.refined.FancyList](smithy4s.example.FancyListFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[FancyList] = bijection(underlyingSchema, asBijection)
}
