package smithy4s.example

import smithy4s.Schema
import smithy4s.example.refined.FancyList
import smithy4s.schema.Schema.list
import smithy4s.Hints
import smithy4s.schema.Schema.string
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object UnwrappedFancyList extends Newtype[FancyList] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnwrappedFancyList")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[FancyList] = list(string).refined[smithy4s.example.refined.FancyList](smithy4s.example.FancyListFormat()).withId(id).addHints(hints)
  implicit val schema : Schema[UnwrappedFancyList] = bijection(underlyingSchema, asBijection)
}