package smithy4s.example

import smithy4s.Hints
import smithy4s.NewtypeValidated
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object UnicodeRegexString extends NewtypeValidated[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnicodeRegexString")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Pattern(s"^\uD83D\uDE0E$$"))
  implicit val schema: Schema[UnicodeRegexString] = bijection(underlyingSchema, asBijectionUnsafe)
  val validators: List[String => Either[String, String]] = List(
    a => validateInternal(smithy.api.Pattern(s"^\uD83D\uDE0E$$"))(a)
  )
  @inline def apply(a: String): Either[String, UnicodeRegexString] = validators
    .foldLeft(Right(a): Either[String, String])((acc, v) => acc.flatMap(v))
    .map(unsafeApply)
}
