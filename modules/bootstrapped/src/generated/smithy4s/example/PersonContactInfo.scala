package smithy4s.example

import smithy4s.Bijection
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait PersonContactInfo extends scala.Product with scala.Serializable {
  @inline final def widen: PersonContactInfo = this
  def _ordinal: Int
}
object PersonContactInfo extends ShapeTag.Companion[PersonContactInfo] {
  final case class EmailCase(email: PersonEmail) extends PersonContactInfo { final def _ordinal: Int = 0 }
  final case class PhoneCase(phone: PersonPhoneNumber) extends PersonContactInfo { final def _ordinal: Int = 1 }

  object EmailCase {
    implicit val fromValue: Bijection[PersonEmail, EmailCase] = Bijection(EmailCase(_), _.email)
    implicit val toValue: Bijection[EmailCase, PersonEmail] = fromValue.swap
    val schema: Schema[EmailCase] = bijection(PersonEmail.schema, fromValue)
  }
  object PhoneCase {
    implicit val fromValue: Bijection[PersonPhoneNumber, PhoneCase] = Bijection(PhoneCase(_), _.phone)
    implicit val toValue: Bijection[PhoneCase, PersonPhoneNumber] = fromValue.swap
    val schema: Schema[PhoneCase] = bijection(PersonPhoneNumber.schema, fromValue)
  }

  val email = EmailCase.schema.oneOf[PersonContactInfo]("email")
  val phone = PhoneCase.schema.oneOf[PersonContactInfo]("phone")

  implicit val schema: Schema[PersonContactInfo] = union(
    email,
    phone,
  ){
    _._ordinal
  }
  .withId(ShapeId("smithy4s.example", "PersonContactInfo"))
  .addHints(
    smithy4s.example.Hash(),
  )

  implicit val personContactInfoHash: cats.Hash[PersonContactInfo] = SchemaVisitorHash.fromSchema(schema)
}
