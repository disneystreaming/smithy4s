package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.list

object ValidatedConstrainedListRefinedMember extends ValidatedNewtype[List[Name]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedConstrainedListRefinedMember")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Name]] = list(Name.schema).withId(id).addHints(hints).validated(smithy.api.Length(min = None, max = Some(1L)))
  val validator: Validator[List[Name], ValidatedConstrainedListRefinedMember] = Validator.Builder.list[Name].validating(smithy.api.Length(min = None, max = Some(1L))).biject(Bijection[List[Name], ValidatedConstrainedListRefinedMember](_.asInstanceOf[ValidatedConstrainedListRefinedMember], value(_))).build()
  implicit val schema: Schema[ValidatedConstrainedListRefinedMember] = validator.toSchema(underlyingSchema)
  @inline def apply(a: List[Name]): Either[String, ValidatedConstrainedListRefinedMember] = validator.validate(a)
}
