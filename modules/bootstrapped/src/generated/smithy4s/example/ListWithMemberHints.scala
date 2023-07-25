package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

/** @param member
  *   listFoo
  */
object ListWithMemberHints extends Newtype[List[String]] {
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(string.addMemberHints(smithy.api.Documentation("listFoo"))).withId(ShapeId("smithy4s.example", "ListWithMemberHints")).addHints(hints)
  implicit val schema: Schema[ListWithMemberHints] = bijection(underlyingSchema, asBijection)
}
