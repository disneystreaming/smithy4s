package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object ValidatedListConstrainedMember extends ValidatedNewtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedListConstrainedMember")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[String]] = list(string.addMemberHints().validated(smithy.api.Length(min = None, max = Some(2L)))).withId(id).addHints(hints)
  val validator: Validator[List[String], ValidatedListConstrainedMember] = Validator.Builder.list[String].validatingMember(smithy.api.Length(min = None, max = Some(2L))).biject(Bijection[List[String], ValidatedListConstrainedMember](_.asInstanceOf[ValidatedListConstrainedMember], value(_))).build()
  implicit val schema: Schema[ValidatedListConstrainedMember] = validator.toSchema(underlyingSchema)
  @inline def apply(a: List[String]): Either[String, ValidatedListConstrainedMember] = validator.validate(a)
}
