package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.optics.Prism
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait PersonContactInfo extends scala.Product with scala.Serializable { self =>
  @inline final def widen: PersonContactInfo = this
  def $ordinal: Int

  object project {
    def email: Option[PersonEmail] = PersonContactInfo.EmailCase.alt.project.lift(self).map(_.email)
    def phone: Option[PersonPhoneNumber] = PersonContactInfo.PhoneCase.alt.project.lift(self).map(_.phone)
  }

  def accept[A](visitor: PersonContactInfo.Visitor[A]): A = this match {
    case value: PersonContactInfo.EmailCase => visitor.email(value.email)
    case value: PersonContactInfo.PhoneCase => visitor.phone(value.phone)
  }
}
object PersonContactInfo extends ShapeTag.Companion[PersonContactInfo] {

  def email(email: PersonEmail): PersonContactInfo = EmailCase(email)
  def phone(phone: PersonPhoneNumber): PersonContactInfo = PhoneCase(phone)

  val id: ShapeId = ShapeId("smithy4s.example", "PersonContactInfo")

  val hints: Hints = Hints(
    smithy4s.example.Hash(),
  )

  object optics {
    val email: Prism[PersonContactInfo, PersonEmail] = Prism.partial[PersonContactInfo, PersonEmail]{ case PersonContactInfo.EmailCase(t) => t }(PersonContactInfo.EmailCase.apply)
    val phone: Prism[PersonContactInfo, PersonPhoneNumber] = Prism.partial[PersonContactInfo, PersonPhoneNumber]{ case PersonContactInfo.PhoneCase(t) => t }(PersonContactInfo.PhoneCase.apply)
  }

  final case class EmailCase(email: PersonEmail) extends PersonContactInfo { final def $ordinal: Int = 0 }
  final case class PhoneCase(phone: PersonPhoneNumber) extends PersonContactInfo { final def $ordinal: Int = 1 }

  object EmailCase {
    val hints: Hints = Hints.empty
    val schema: Schema[PersonContactInfo.EmailCase] = bijection(PersonEmail.schema.addHints(hints), PersonContactInfo.EmailCase(_), _.email)
    val alt = schema.oneOf[PersonContactInfo]("email")
  }
  object PhoneCase {
    val hints: Hints = Hints.empty
    val schema: Schema[PersonContactInfo.PhoneCase] = bijection(PersonPhoneNumber.schema.addHints(hints), PersonContactInfo.PhoneCase(_), _.phone)
    val alt = schema.oneOf[PersonContactInfo]("phone")
  }

  trait Visitor[A] {
    def email(value: PersonEmail): A
    def phone(value: PersonPhoneNumber): A
  }

  implicit val schema: Schema[PersonContactInfo] = union(
    PersonContactInfo.EmailCase.alt,
    PersonContactInfo.PhoneCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)

  implicit val personContactInfoHash: cats.Hash[PersonContactInfo] = SchemaVisitorHash.fromSchema(schema)
}
