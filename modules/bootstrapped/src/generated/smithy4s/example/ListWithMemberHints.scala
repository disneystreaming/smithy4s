package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

/** @param member
  *   listFoo
  */
object ListWithMemberHints extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ListWithMemberHints")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(string.addMemberHints(smithy.api.Documentation("listFoo"))).withId(id).addHints(hints)
  implicit val schema: Schema[ListWithMemberHints] = bijection(underlyingSchema, asBijection)
}
