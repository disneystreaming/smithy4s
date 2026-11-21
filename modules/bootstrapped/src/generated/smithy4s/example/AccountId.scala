package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.string

object AccountId extends ValidatedNewtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "AccountId")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Pattern("[a-zA-Z0-9]+"))
  val validator: Validator[String, AccountId] = Validator.Builder.simple[String].validating(smithy.api.Pattern("[a-zA-Z0-9]+")).biject(Bijection[String, AccountId](_.asInstanceOf[AccountId], value(_))).build()
  implicit val schema: Schema[AccountId] = validator.toSchema(underlyingSchema)
  @inline def apply(a: String): Either[String, AccountId] = validator.validate(a)
}
