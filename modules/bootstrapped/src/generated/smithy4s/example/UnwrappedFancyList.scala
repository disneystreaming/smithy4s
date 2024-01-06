package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object UnwrappedFancyList extends Newtype[smithy4s.refined.FancyList] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnwrappedFancyList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[smithy4s.refined.FancyList] = list(string).refined[smithy4s.refined.FancyList](smithy4s.example.FancyListFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[UnwrappedFancyList] = bijection(underlyingSchema, asBijection)
}
