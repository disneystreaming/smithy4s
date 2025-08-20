package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.refined.NonEmptyList
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object NonEmptyStrings extends Newtype[NonEmptyList[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "NonEmptyStrings")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example", "nonEmptyListFormat"), smithy4s.Document.obj()),
  ).lazily
  val underlyingSchema: Schema[NonEmptyList[String]] = list(string).refined[NonEmptyList[String]](smithy4s.example.NonEmptyListFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[NonEmptyStrings] = bijection(underlyingSchema, asBijection)
}
