package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.set
import smithy4s.schema.Schema.string

object StringSet extends Newtype[Set[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "StringSet")
  val hints: Hints = Hints(
    smithy.api.UniqueItems(),
  )
  val underlyingSchema: Schema[Set[String]] = set(string).withId(id).addHints(hints)
  implicit val schema: Schema[StringSet] = bijection(underlyingSchema, asBijection)
}
