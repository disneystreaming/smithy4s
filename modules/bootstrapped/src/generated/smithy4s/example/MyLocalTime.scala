package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.localtime
import smithy4s.time.LocalTime

object MyLocalTime extends Newtype[LocalTime] {
  val id: ShapeId = ShapeId("smithy4s.example", "MyLocalTime")
  val hints: Hints = Hints(
    alloy.LocalTimeFormat(),
  ).lazily
  val underlyingSchema: Schema[LocalTime] = localtime.withId(id).addHints(hints)
  implicit val schema: Schema[MyLocalTime] = bijection(underlyingSchema, asBijection)
}
