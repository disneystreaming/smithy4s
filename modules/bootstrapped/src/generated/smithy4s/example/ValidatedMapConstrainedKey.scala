package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object ValidatedMapConstrainedKey extends ValidatedNewtype[Map[String, Int]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedMapConstrainedKey")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, Int]] = map(string.addMemberHints().validated(smithy.api.Length(min = None, max = Some(2L))), int).withId(id).addHints(hints)
  val validator: Validator[Map[String, Int], ValidatedMapConstrainedKey] = Validator.Builder.map[String, Int].validatingKey(smithy.api.Length(min = None, max = Some(2L))).biject(Bijection[Map[String, Int], ValidatedMapConstrainedKey](_.asInstanceOf[ValidatedMapConstrainedKey], value(_))).build()
  implicit val schema: Schema[ValidatedMapConstrainedKey] = validator.toSchema(underlyingSchema)
  @inline def apply(a: Map[String, Int]): Either[String, ValidatedMapConstrainedKey] = validator.validate(a)
}
