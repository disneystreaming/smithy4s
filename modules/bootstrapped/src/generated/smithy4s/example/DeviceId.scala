package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.string

object DeviceId extends ValidatedNewtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "DeviceId")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Length(min = Some(1L), max = None))
  val validator: Validator[String, DeviceId] = Validator.Builder.simple[String].validating(smithy.api.Length(min = Some(1L), max = None)).biject(Bijection[String, DeviceId](_.asInstanceOf[DeviceId], value(_))).build()
  implicit val schema: Schema[DeviceId] = validator.toSchema(underlyingSchema)
  @inline def apply(a: String): Either[String, DeviceId] = validator.validate(a)
}
