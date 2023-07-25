package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.set
import smithy4s.schema.Schema.string

object StringSet extends Newtype[Set[String]] {
  val hints: Hints = Hints(
    smithy.api.UniqueItems(),
  )
  val underlyingSchema: Schema[Set[String]] = set(string).withId(ShapeId("smithy4s.example", "StringSet")).addHints(hints)
  implicit val schema: Schema[StringSet] = bijection(underlyingSchema, asBijection)
}
