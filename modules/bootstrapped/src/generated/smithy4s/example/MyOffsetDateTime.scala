package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.offsetdatetime
import smithy4s.time.OffsetDateTime

object MyOffsetDateTime extends Newtype[OffsetDateTime] {
  val id: ShapeId = ShapeId("smithy4s.example", "MyOffsetDateTime")
  val hints: Hints = Hints(
    alloy.OffsetDateTimeFormat(),
    smithy.api.TimestampFormat.DATE_TIME.widen,
  ).lazily
  val underlyingSchema: Schema[OffsetDateTime] = offsetdatetime.withId(id).addHints(hints)
  implicit val schema: Schema[MyOffsetDateTime] = bijection(underlyingSchema, asBijection)
}
