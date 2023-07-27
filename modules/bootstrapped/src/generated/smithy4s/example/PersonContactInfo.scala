package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.optics.Prism
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait PersonContactInfo extends scala.Product with scala.Serializable {
  @inline final def widen: PersonContactInfo = this
  def _ordinal: Int
}
object PersonContactInfo extends ShapeTag.Companion[PersonContactInfo] {

  def email(email:PersonEmail): PersonContactInfo = EmailCase(email)
  def phone(phone:PersonPhoneNumber): PersonContactInfo = PhoneCase(phone)

  val id: ShapeId = ShapeId("smithy4s.example", "PersonContactInfo")

  val hints: Hints = Hints(
    smithy4s.example.Hash(),
  )

  object optics {
    val email: Prism[PersonContactInfo, PersonEmail] = Prism.partial[PersonContactInfo, PersonEmail]{ case EmailCase(t) => t }(EmailCase.apply)
    val phone: Prism[PersonContactInfo, PersonPhoneNumber] = Prism.partial[PersonContactInfo, PersonPhoneNumber]{ case PhoneCase(t) => t }(PhoneCase.apply)
  }

  final case class EmailCase(email: PersonEmail) extends PersonContactInfo { final def _ordinal: Int = 0 }
  final case class PhoneCase(phone: PersonPhoneNumber) extends PersonContactInfo { final def _ordinal: Int = 1 }

  object EmailCase {
    val hints: Hints = Hints.empty
    val schema: Schema[EmailCase] = bijection(PersonEmail.schema.addHints(hints), EmailCase(_), _.email)
    val alt = schema.oneOf[PersonContactInfo]("email")
  }
  object PhoneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[PhoneCase] = bijection(PersonPhoneNumber.schema.addHints(hints), PhoneCase(_), _.phone)
    val alt = schema.oneOf[PersonContactInfo]("phone")
  }

  implicit val schema: Schema[PersonContactInfo] = union(
    EmailCase.alt,
    PhoneCase.alt,
  ){
    _._ordinal
  }.withId(id).addHints(hints)

  implicit val personContactInfoHash: cats.Hash[PersonContactInfo] = SchemaVisitorHash.fromSchema(schema)
}
