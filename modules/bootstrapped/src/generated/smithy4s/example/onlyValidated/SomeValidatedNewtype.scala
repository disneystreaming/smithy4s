package smithy4s.example.onlyValidated

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.string

object SomeValidatedNewtype extends ValidatedNewtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example.onlyValidated", "SomeValidatedNewtype")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None))
  val validator: Validator[String, SomeValidatedNewtype] = Validator.of[String, SomeValidatedNewtype](Bijection[String, SomeValidatedNewtype](_.asInstanceOf[SomeValidatedNewtype], value(_))).validating(smithy.api.Length(min = Some(1L), max = None))
  implicit val schema: Schema[SomeValidatedNewtype] = validator.toSchema(underlyingSchema)
  @inline def apply(a: String): Either[String, SomeValidatedNewtype] = validator.validate(a)
}
