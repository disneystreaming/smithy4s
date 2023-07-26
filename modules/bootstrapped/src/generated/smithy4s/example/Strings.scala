package smithy4s.example

import smithy.api.Deprecated
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

@deprecated(message = "N/A", since = "N/A")
object Strings extends Newtype[List[String]] {
  val underlyingSchema: Schema[List[String]] = list(string)
  .withId(ShapeId("smithy4s.example", "Strings"))
  .addHints(
    Deprecated(message = None, since = None),
  )

  implicit val schema: Schema[Strings] = bijection(underlyingSchema, asBijection)
}
