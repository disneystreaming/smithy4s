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

object ValidatedConstrainedMap extends ValidatedNewtype[Map[String, Int]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedConstrainedMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, Int]] = map(string, int).withId(id).addHints(hints).validated(smithy.api.Length(min = None, max = Some(1L)))
  val validator: Validator[Map[String, Int], ValidatedConstrainedMap] = Validator.Builder.map[String, Int].validating(smithy.api.Length(min = None, max = Some(1L))).biject(Bijection[Map[String, Int], ValidatedConstrainedMap](_.asInstanceOf[ValidatedConstrainedMap], value(_))).build()
  implicit val schema: Schema[ValidatedConstrainedMap] = validator.toSchema(underlyingSchema)
  @inline def apply(a: Map[String, Int]): Either[String, ValidatedConstrainedMap] = validator.validate(a)
}
