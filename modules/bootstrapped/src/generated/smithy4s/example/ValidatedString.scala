package smithy4s.example

import smithy4s.Hints
import smithy4s.NewtypeValidated
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object ValidatedString extends NewtypeValidated[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedString")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None)).validated(smithy.api.Pattern("[a-zA-Z0-9]+"))
  implicit val schema: Schema[ValidatedString] = bijection(underlyingSchema, asBijectionUnsafe)
  val validators: List[String => Either[String, String]] = List(
    a => validateInternal(smithy.api.Length(min = Some(1L), max = None))(a), a => validateInternal(smithy.api.Pattern("[a-zA-Z0-9]+"))(a)
  )
  @inline def apply(a: String): Either[String, ValidatedString] = validators
    .foldLeft(Right(a): Either[String, String])((acc, v) => acc.flatMap(v))
    .map(unsafeApply)
}
