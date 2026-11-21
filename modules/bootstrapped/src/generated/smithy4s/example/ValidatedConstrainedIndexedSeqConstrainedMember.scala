package smithy4s.example

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ValidatedNewtype
import smithy4s.Validator
import smithy4s.schema.Schema.indexedSeq
import smithy4s.schema.Schema.string

object ValidatedConstrainedIndexedSeqConstrainedMember extends ValidatedNewtype[IndexedSeq[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "ValidatedConstrainedIndexedSeqConstrainedMember")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[IndexedSeq[String]] = indexedSeq(string.addMemberHints().validated(smithy.api.Length(min = None, max = Some(2L)))).withId(id).addHints(hints).validated(smithy.api.Length(min = None, max = Some(1L)))
  val validator: Validator[IndexedSeq[String], ValidatedConstrainedIndexedSeqConstrainedMember] = Validator.Builder.indexedSeq[String].validating(smithy.api.Length(min = None, max = Some(1L))).validatingMember(smithy.api.Length(min = None, max = Some(2L))).biject(Bijection[IndexedSeq[String], ValidatedConstrainedIndexedSeqConstrainedMember](_.asInstanceOf[ValidatedConstrainedIndexedSeqConstrainedMember], value(_))).build()
  implicit val schema: Schema[ValidatedConstrainedIndexedSeqConstrainedMember] = validator.toSchema(underlyingSchema)
  @inline def apply(a: IndexedSeq[String]): Either[String, ValidatedConstrainedIndexedSeqConstrainedMember] = validator.validate(a)
}
