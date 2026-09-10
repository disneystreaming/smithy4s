package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object ValidatedConstrainedList extends ValidatedNewtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedConstrainedList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(string).withId(id).addHints(hints).validated(smithy.api.Length(min = None, max = Some(1L)))
  val validator: Validator[List[String], ValidatedConstrainedList] = Validator.Builder.list[String].validating(smithy.api.Length(min = None, max = Some(1L))).biject(Bijection[List[String], ValidatedConstrainedList](_.asInstanceOf[ValidatedConstrainedList], value(_))).build()
  implicit val schema: Schema[ValidatedConstrainedList] = validator.toSchema(underlyingSchema)
  @inline def apply(a: List[String]): Either[String, ValidatedConstrainedList] = validator.validate(a)
}
