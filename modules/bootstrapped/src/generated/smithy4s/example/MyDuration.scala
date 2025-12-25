package smithy4s.example

import scala.concurrent.duration.Duration
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.duration

object MyDuration extends Newtype[Duration] {
  val id: ShapeId = ShapeId("smithy4s.example", "MyDuration")
  val hints: Hints = Hints(
    alloy.DurationSecondsFormat(),
  ).lazily
  val underlyingSchema: Schema[Duration] = duration.withId(id).addHints(hints)
  implicit val schema: Schema[MyDuration] = bijection(underlyingSchema, asBijection)
}
