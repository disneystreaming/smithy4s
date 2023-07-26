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
  val underlyingSchema: Schema[NonEmptyMap[String, Int]] = map(string, int).refined[NonEmptyMap[String, Int]](NonEmptyMapFormat())
  .withId(ShapeId("smithy4s.example", "NonEmptyMapNumbers"))
  .addHints(
    Hints.empty
  )

  implicit val schema: Schema[NonEmptyMapNumbers] = bijection(underlyingSchema, asBijection)
}
