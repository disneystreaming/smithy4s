package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.list
import smithy4s.Hints
import smithy4s.schema.Schema.string
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

@deprecated
object Strings extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "Strings")
  val hints : Hints = Hints(
    smithy.api.Deprecated(message = None, since = None),
  )
  val underlyingSchema : Schema[List[String]] = list(string).withId(id).addHints(hints)
  implicit val schema : Schema[Strings] = bijection(underlyingSchema, asBijection)
}