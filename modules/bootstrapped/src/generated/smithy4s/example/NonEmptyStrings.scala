package smithy4s.example

import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.refined.NonEmptyList
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object NonEmptyStrings extends Newtype[NonEmptyList[String]] {
  val underlyingSchema: Schema[NonEmptyList[String]] = list(string).refined[NonEmptyList[String]](NonEmptyListFormat())
  .withId(ShapeId("smithy4s.example", "NonEmptyStrings"))

  implicit val schema: Schema[NonEmptyStrings] = bijection(underlyingSchema, asBijection)
}
