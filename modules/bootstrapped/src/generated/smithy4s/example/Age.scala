package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.refined.Age.provider._
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object Age extends Newtype[smithy4s.refined.Age] {
  val id: ShapeId = ShapeId("smithy4s.example", "Age")
  val hints: Hints = Hints.lazily(
    Hints(
      smithy.api.Default(smithy4s.Document.fromDouble(0.0d)),
    )
  )
  val underlyingSchema: Schema[smithy4s.refined.Age] = int.refined[smithy4s.refined.Age](smithy4s.example.AgeFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[Age] = bijection(underlyingSchema, asBijection)
}
