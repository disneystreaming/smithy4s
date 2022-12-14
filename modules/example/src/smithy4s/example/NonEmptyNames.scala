package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.example.refined.NonEmptyList
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object NonEmptyNames extends Newtype[NonEmptyList[Name]] {
  val id: ShapeId = ShapeId("smithy4s.example", "NonEmptyNames")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[NonEmptyList[Name]] = list(Name.schema).refined[NonEmptyList[Name]](smithy4s.example.NonEmptyListFormat()).withId(id).addHints(hints)
  implicit val schema : Schema[NonEmptyNames] = bijection(underlyingSchema, asBijection)
}