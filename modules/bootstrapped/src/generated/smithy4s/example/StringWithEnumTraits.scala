package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object StringWithEnumTraits extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "StringWithEnumTraits")
  val hints: Hints = Hints(
    smithy4s.example.OldStyleLeftRight.RIGHT.widen,
    smithy4s.example.OneTwo.ONE.widen,
    smithy4s.example.LeftRight.LEFT.widen,
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[StringWithEnumTraits] = bijection(underlyingSchema, asBijection)
}
