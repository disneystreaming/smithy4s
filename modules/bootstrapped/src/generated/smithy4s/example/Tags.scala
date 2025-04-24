package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object Tags extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "Tags")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(string.addMemberHints().validated(smithy.api.Length(min = Some(1L), max = Some(10L)))).withId(id).addHints(hints)
  implicit val schema: Schema[Tags] = bijection(underlyingSchema, asBijection)
}
