package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.example.instances._
import smithy4s.schema.Schema.vector


object ValidatedConstrainedVectorRefinedConstrainedMember extends ValidatedNewtype[Vector[Name]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedConstrainedVectorRefinedConstrainedMember")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Vector[Name]] = vector(Name.schema.addMemberHints().validated(smithy.api.Length(min = None, max = Some(2L)))).withId(id).addHints(hints).validated(smithy.api.Length(min = None, max = Some(1L)))
  val validator: Validator[Vector[Name], ValidatedConstrainedVectorRefinedConstrainedMember] = Validator.Builder.vector[Name].validating(smithy.api.Length(min = None, max = Some(1L))).validatingMember(smithy.api.Length(min = None, max = Some(2L))).biject(Bijection[Vector[Name], ValidatedConstrainedVectorRefinedConstrainedMember](_.asInstanceOf[ValidatedConstrainedVectorRefinedConstrainedMember], value(_))).build()
  implicit val schema: Schema[ValidatedConstrainedVectorRefinedConstrainedMember] = validator.toSchema(underlyingSchema)
  @inline def apply(a: Vector[Name]): Either[String, ValidatedConstrainedVectorRefinedConstrainedMember] = validator.validate(a)
}
