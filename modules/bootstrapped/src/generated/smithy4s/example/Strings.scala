package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

@deprecated(message = "N/A", since = "N/A")
object Strings extends Newtype[List[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "Strings")
  val hints: Hints = Hints(
    smithy.api.Deprecated(message = None, since = None),
  )
  val underlyingSchema: Schema[List[String]] = list(string).withId(id).addHints(hints)
  implicit val schema: Schema[Strings] = bijection(underlyingSchema, asBijection)
}
