package smithy4s.example

import smithy.api.Default
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.refined.Age.provider._
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object PersonAge extends Newtype[smithy4s.refined.Age] {
  val underlyingSchema: Schema[smithy4s.refined.Age] = int.refined[smithy4s.refined.Age](AgeFormat())
  .withId(ShapeId("smithy4s.example", "PersonAge"))
  .addHints(
    Hints(
      Default(smithy4s.Document.fromDouble(0.0d)),
    )
  )

  implicit val schema: Schema[PersonAge] = bijection(underlyingSchema, asBijection)
}
