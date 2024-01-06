package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.refined.NonEmptyList
import smithy4s.schema.Schema.list

object NonEmptyNames extends Newtype[NonEmptyList[Name]] {
  val id: ShapeId = ShapeId("smithy4s.example", "NonEmptyNames")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[NonEmptyList[Name]] = list(Name.schema).refined[NonEmptyList[Name]](smithy4s.example.NonEmptyListFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[NonEmptyNames] = bijection(underlyingSchema, asBijection)
}
