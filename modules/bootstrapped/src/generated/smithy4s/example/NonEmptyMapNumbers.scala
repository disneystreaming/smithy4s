package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.refined.NonEmptyMap
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object NonEmptyMapNumbers extends Newtype[NonEmptyMap[String, Int]] {
  val id: ShapeId = ShapeId("smithy4s.example", "NonEmptyMapNumbers")
  val hints: Hints = Hints(
    Hints.Binding.DynamicBinding(ShapeId("smithy4s.example", "nonEmptyMapFormat"), smithy4s.Document.obj()),
  ).lazily
  val underlyingSchema: Schema[NonEmptyMap[String, Int]] = map(string, int).refined[NonEmptyMap[String, Int]](smithy4s.example.NonEmptyMapFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[NonEmptyMapNumbers] = bijection(underlyingSchema, asBijection)
}
