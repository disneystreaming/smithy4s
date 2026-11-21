package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.string

object ValidatedString extends ValidatedNewtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedString")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None)).validated(smithy.api.Pattern("[a-zA-Z0-9]+"))
  val validator: Validator[String, ValidatedString] = Validator.Builder.simple[String].validating(smithy.api.Length(min = Some(1L), max = None)).validating(smithy.api.Pattern("[a-zA-Z0-9]+")).biject(Bijection[String, ValidatedString](_.asInstanceOf[ValidatedString], value(_))).build()
  implicit val schema: Schema[ValidatedString] = validator.toSchema(underlyingSchema)
  @inline def apply(a: String): Either[String, ValidatedString] = validator.validate(a)
}
