package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.localdate
import smithy4s.time.LocalDate

object MyLocalDate extends Newtype[LocalDate] {
  val id: ShapeId = ShapeId("smithy4s.example", "MyLocalDate")
  val hints: Hints = Hints(
    alloy.DateFormat(),
  ).lazily
  val underlyingSchema: Schema[LocalDate] = localdate.withId(id).addHints(hints)
  implicit val schema: Schema[MyLocalDate] = bijection(underlyingSchema, asBijection)
}
