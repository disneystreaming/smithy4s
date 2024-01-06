package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object StringList extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "StringList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(string).withId(id).addHints(hints)
  implicit val schema: Schema[StringList] = bijection(underlyingSchema, asBijection)
}
