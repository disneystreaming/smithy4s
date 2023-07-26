package smithy4s.example

import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.refined.NonEmptyList
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object NonEmptyNames extends Newtype[NonEmptyList[Name]] {
  val underlyingSchema: Schema[NonEmptyList[Name]] = list(Name.schema).refined[NonEmptyList[Name]](NonEmptyListFormat())
  .withId(ShapeId("smithy4s.example", "NonEmptyNames"))

  implicit val schema: Schema[NonEmptyNames] = bijection(underlyingSchema, asBijection)
}
