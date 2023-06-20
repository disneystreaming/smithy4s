package aws.api

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

/** Points to an operation designated for a tagging APi */
object TagOperationReference extends Newtype[String] {
  val id: ShapeId = ShapeId("aws.api", "TagOperationReference")
  val hints: Hints = Hints(
    smithy.api.Documentation("Points to an operation designated for a tagging APi"),
    smithy.api.IdRef(selector = "resource > operation", failWhenMissing = Some(true), errorMessage = None),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[TagOperationReference] = bijection(underlyingSchema, asBijection)
}
