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

object ValidatedMapConstrainedValue extends ValidatedNewtype[Map[String, Int]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedMapConstrainedValue")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, Int]] = map(string, int.addMemberHints().validated(smithy.api.Range(min = None, max = Some(scala.math.BigDecimal(2.0))))).withId(id).addHints(hints)
  val validator: Validator[Map[String, Int], ValidatedMapConstrainedValue] = Validator.Builder.map[String, Int].validatingValue(smithy.api.Range(min = None, max = Some(scala.math.BigDecimal(2.0)))).biject(Bijection[Map[String, Int], ValidatedMapConstrainedValue](_.asInstanceOf[ValidatedMapConstrainedValue], value(_))).build()
  implicit val schema: Schema[ValidatedMapConstrainedValue] = validator.toSchema(underlyingSchema)
  @inline def apply(a: Map[String, Int]): Either[String, ValidatedMapConstrainedValue] = validator.validate(a)
}
