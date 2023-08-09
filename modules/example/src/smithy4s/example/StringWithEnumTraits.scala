package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
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