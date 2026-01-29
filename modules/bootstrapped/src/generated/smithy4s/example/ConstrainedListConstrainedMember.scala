package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object ConstrainedListConstrainedMember extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ConstrainedListConstrainedMember")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(string.addMemberHints().validated(smithy.api.Length(min = None, max = Some(2L)))).withId(id).addHints(hints).validated(smithy.api.Length(min = None, max = Some(1L)))
  implicit val schema: Schema[ConstrainedListConstrainedMember] = bijection(underlyingSchema, asBijection)
}
