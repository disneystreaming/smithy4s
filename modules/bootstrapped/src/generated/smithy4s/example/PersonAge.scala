package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.refined.Age.provider._
import smithy4s.schema.Schema.int

object PersonAge extends Newtype[smithy4s.refined.Age] {
  val id: ShapeId = ShapeId("smithy4s.example", "PersonAge")
  val hints: Hints = Hints(
    smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d)),
  )
  val underlyingSchema: Schema[smithy4s.refined.Age] = int.refined[smithy4s.refined.Age](smithy4s.example.AgeFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[PersonAge] = bijection(underlyingSchema, asBijection)
}
