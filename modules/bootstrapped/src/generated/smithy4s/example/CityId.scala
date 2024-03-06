package smithy4s.example

import smithy4s.Hints
import smithy4s.NewtypeValidated
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object CityId extends NewtypeValidated[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "CityId")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Pattern(s"^[A-Za-z0-9 ]+$$"))
  implicit val schema: Schema[CityId] = bijection(underlyingSchema, asBijectionUnsafe)
  val validators: List[String => Either[String, String]] = List(
    a => validateInternal(smithy.api.Pattern(s"^[A-Za-z0-9 ]+$$"))(a)
  )
  @inline def apply(a: String): Either[String, CityId] = validators
    .foldLeft(Right(a): Either[String, String])((acc, v) => acc.flatMap(v))
    .map(unsafeApply)
}
