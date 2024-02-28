// just for experimentation
package smithy4s.refined.handwritten

import smithy4s.NewtypeValidated
import smithy4s.schema.Schema.string
import smithy4s.RefinementProvider

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class EmailFormat()

object EmailFormat extends ShapeTag.Companion[EmailFormat] {
  val id: ShapeId = ShapeId("input", "emailFormat")

  val hints: Hints = Hints(
    smithy.api.Trait(
      selector = Some("string"),
      structurallyExclusive = None,
      conflicts = None,
      breakingChanges = None
    )
  )

  implicit val schema: Schema[EmailFormat] =
    constant(EmailFormat()).withId(id).addHints(hints)
}
object NonEmptyString extends NewtypeValidated[String] {

  protected val refinement =
    RefinementProvider.make[String](
      /* needs to be generated */
      smithy.api.Length(min = Some(1L), max = None)
    )

  def apply(a: String): Either[String, Type] =
    refinement
      .apply(a)
      .map(unsafeApply)

  val id: ShapeId = ShapeId("input", "NonEmptyString")
  val hints: Hints = Hints.empty

  val underlyingSchema: Schema[String] = Schema
    .RefinementSchema(
      string.withId(id).addHints(hints),
      refinement
    )

  implicit val schema: Schema[NonEmptyString] =
    underlyingSchema
      .biject(asBijectionUnsafe)
}

case class EmailScala private (value: String)
object EmailScala {

  implicit val provider: RefinementProvider[EmailFormat, String, EmailScala] =
    null
}

import EmailScala.provider

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.string

object EmailSmithy extends NewtypeValidated[EmailScala] {

  private val refinement = RefinementProvider.make[String](
    EmailFormat()
  )

  def apply(a: EmailScala): Either[String, Type] =
    refinement
      .apply(a.value)
      .map(unsafeApply)

  val id: ShapeId = ShapeId("input", "Email")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[EmailScala] = string
    .refined[EmailScala](EmailFormat())
    .withId(id)
    .addHints(hints)

  implicit val schema: Schema[EmailSmithy] =
    underlyingSchema.biject(asBijectionUnsafe)
}
