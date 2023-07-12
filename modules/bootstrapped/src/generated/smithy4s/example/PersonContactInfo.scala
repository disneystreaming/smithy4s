package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait PersonContactInfo extends scala.Product with scala.Serializable {
  @inline final def widen: PersonContactInfo = this
}
object PersonContactInfo extends ShapeTag.Companion[PersonContactInfo] {
  val id: ShapeId = ShapeId("smithy4s.example", "PersonContactInfo")

  val hints: Hints = Hints(
    smithy4s.example.Hash(),
  )

  final case class EmailCase(email: PersonEmail) extends PersonContactInfo
  def emailCase(emailCase:PersonEmail): PersonContactInfo = EmailCase(emailCase)
  final case class PhoneCase(phone: PersonPhoneNumber) extends PersonContactInfo
  def phoneCase(phoneCase:PersonPhoneNumber): PersonContactInfo = PhoneCase(phoneCase)

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
    case c: EmailCase => EmailCase.alt(c)
    case c: PhoneCase => PhoneCase.alt(c)
  }.withId(id).addHints(hints)

  implicit val personContactInfoHash: cats.Hash[PersonContactInfo] = SchemaVisitorHash.fromSchema(schema)
}
