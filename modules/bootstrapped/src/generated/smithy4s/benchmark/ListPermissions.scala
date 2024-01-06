package smithy4s.benchmark

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object ListPermissions extends Newtype[List[Permission]] {
  val id: ShapeId = ShapeId("smithy4s.benchmark", "ListPermissions")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Permission]] = list(Permission.schema).withId(id).addHints(hints)
  implicit val schema: Schema[ListPermissions] = bijection(underlyingSchema, asBijection)
}
