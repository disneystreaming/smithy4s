package aws.api

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

/** A string representing a CloudFormation service name. */
object CloudFormationName extends Newtype[String] {
  val id: ShapeId = ShapeId("aws.api", "CloudFormationName")
  val hints: Hints = Hints(
    smithy.api.Documentation("A string representing a CloudFormation service name."),
    smithy.api.Private(),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Pattern("^[A-Z][A-Za-z0-9]+$"))
  implicit val schema: Schema[CloudFormationName] = bijection(underlyingSchema, asBijection)
}
