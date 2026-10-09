package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object ValidatedMapConstrainedValue extends ValidatedNewtype[Map[String, String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedMapConstrainedValue")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, String]] = map(string, string.addMemberHints().validated(smithy.api.Pattern(s"^[a-zA-Z0-9]+$$"))).withId(id).addHints(hints)
  val validator: Validator[Map[String, String], ValidatedMapConstrainedValue] = Validator.Builder.map[String, String].validatingValue(smithy.api.Pattern(s"^[a-zA-Z0-9]+$$")).biject(Bijection[Map[String, String], ValidatedMapConstrainedValue](_.asInstanceOf[ValidatedMapConstrainedValue], value(_))).build()
  implicit val schema: Schema[ValidatedMapConstrainedValue] = validator.toSchema(underlyingSchema)
  @inline def apply(a: Map[String, String]): Either[String, ValidatedMapConstrainedValue] = validator.validate(a)
}
