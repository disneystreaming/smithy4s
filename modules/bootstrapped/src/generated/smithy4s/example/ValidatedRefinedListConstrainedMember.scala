package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.refined.NonEmptyList
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object ValidatedRefinedListConstrainedMember extends ValidatedNewtype[NonEmptyList[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedRefinedListConstrainedMember")
  val hints: Hints = Hints(
    smithy4s.example.NonEmptyListFormat(),
  ).lazily
  val underlyingSchema: Schema[NonEmptyList[String]] = list(string.addMemberHints().validated(smithy.api.Length(min = None, max = Some(2L)))).refined[NonEmptyList[String]](smithy4s.example.NonEmptyListFormat()).withId(id).addHints(hints)
  val validator: Validator[NonEmptyList[String], ValidatedRefinedListConstrainedMember] = Validator.Builder.list[String].validatingMember(smithy.api.Length(min = None, max = Some(2L))).refined[NonEmptyList[String]](smithy4s.example.NonEmptyListFormat()).biject(Bijection[NonEmptyList[String], ValidatedRefinedListConstrainedMember](_.asInstanceOf[ValidatedRefinedListConstrainedMember], value(_))).build()
  implicit val schema: Schema[ValidatedRefinedListConstrainedMember] = validator.toSchema(underlyingSchema)
  @inline def apply(a: NonEmptyList[String]): Either[String, ValidatedRefinedListConstrainedMember] = validator.validate(a)
}
