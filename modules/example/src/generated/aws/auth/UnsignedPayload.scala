package aws.auth

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** Indicates that the request payload of a signed request is not to be used
  * as part of the signature.
  */
final case class UnsignedPayload()
object UnsignedPayload extends ShapeTag.Companion[UnsignedPayload] {
  val id: ShapeId = ShapeId("aws.auth", "unsignedPayload")

  val hints: Hints = Hints(
    smithy.api.Documentation("Indicates that the request payload of a signed request is not to be used\nas part of the signature."),
    smithy.api.Trait(selector = Some("operation"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[UnsignedPayload] = constant(UnsignedPayload()).withId(id).addHints(hints)
}
