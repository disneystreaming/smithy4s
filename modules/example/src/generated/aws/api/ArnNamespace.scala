package aws.api

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

/** A string representing a service's ARN namespace. */
object ArnNamespace extends Newtype[String] {
  val id: ShapeId = ShapeId("aws.api", "ArnNamespace")
  val hints: Hints = Hints(
    smithy.api.Documentation("A string representing a service\'s ARN namespace."),
    smithy.api.Private(),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints).validated(smithy.api.Pattern("^[a-z0-9.\\-]{1,63}$"))
  implicit val schema: Schema[ArnNamespace] = bijection(underlyingSchema, asBijection)
}
