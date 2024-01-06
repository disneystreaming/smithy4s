package smithy4s.example.collision

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object String extends Newtype[_root_.java.lang.String] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "String")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[_root_.java.lang.String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[String] = bijection(underlyingSchema, asBijection)
}
