package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object Name extends Newtype[smithy4s.refined.Name] {
  val id: ShapeId = ShapeId("smithy4s.example", "Name")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[smithy4s.refined.Name] = string.refined[smithy4s.refined.Name](smithy4s.example.NameFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[Name] = bijection(underlyingSchema, asBijection)
}
