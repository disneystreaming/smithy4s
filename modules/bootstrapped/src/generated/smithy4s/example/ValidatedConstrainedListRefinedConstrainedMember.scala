package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.example.instances._
import smithy4s.schema.Schema.list


object ValidatedConstrainedListRefinedConstrainedMember extends ValidatedNewtype[List[Name]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedConstrainedListRefinedConstrainedMember")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Name]] = list(Name.schema.addMemberHints().validated(smithy.api.Length(min = None, max = Some(2L)))).withId(id).addHints(hints).validated(smithy.api.Length(min = None, max = Some(1L)))
  val validator: Validator[List[Name], ValidatedConstrainedListRefinedConstrainedMember] = Validator.Builder.list[Name].validating(smithy.api.Length(min = None, max = Some(1L))).validatingMember(smithy.api.Length(min = None, max = Some(2L))).biject(Bijection[List[Name], ValidatedConstrainedListRefinedConstrainedMember](_.asInstanceOf[ValidatedConstrainedListRefinedConstrainedMember], value(_))).build()
  implicit val schema: Schema[ValidatedConstrainedListRefinedConstrainedMember] = validator.toSchema(underlyingSchema)
  @inline def apply(a: List[Name]): Either[String, ValidatedConstrainedListRefinedConstrainedMember] = validator.validate(a)
}
