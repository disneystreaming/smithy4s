package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.set
import smithy4s.schema.Schema.string

object ValidatedSetConstrainedMember extends ValidatedNewtype[Set[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedSetConstrainedMember")
  val hints: Hints = Hints(
    smithy.api.UniqueItems(),
  ).lazily
  val underlyingSchema: Schema[Set[String]] = set(string.addMemberHints().validated(smithy.api.Length(min = None, max = Some(2L)))).withId(id).addHints(hints)
  val validator: Validator[Set[String], ValidatedSetConstrainedMember] = Validator.Builder.set[String].validatingMember(smithy.api.Length(min = None, max = Some(2L))).biject(Bijection[Set[String], ValidatedSetConstrainedMember](_.asInstanceOf[ValidatedSetConstrainedMember], value(_))).build()
  implicit val schema: Schema[ValidatedSetConstrainedMember] = validator.toSchema(underlyingSchema)
  @inline def apply(a: Set[String]): Either[String, ValidatedSetConstrainedMember] = validator.validate(a)
}
