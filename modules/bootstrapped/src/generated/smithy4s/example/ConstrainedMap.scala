package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object ConstrainedMap extends Newtype[Map[String, String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ConstrainedMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, String]] = map(string.addMemberHints().validated(smithy.api.Length(min = Some(2L), max = Some(12L))), string.addMemberHints().validated(smithy.api.Length(min = Some(3L), max = Some(13L)))).withId(id).addHints(hints)
  implicit val schema: Schema[ConstrainedMap] = bijection(underlyingSchema, asBijection)
}
