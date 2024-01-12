package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

@deprecated(message = "N/A", since = "N/A")
object DeprecatedString extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "DeprecatedString")
  val hints: Hints = Hints(
    smithy.api.Deprecated(message = None, since = None),
  ).lazily
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[DeprecatedString] = bijection(underlyingSchema, asBijection)
}
