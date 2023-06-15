package smithy.test

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object StringMap extends Newtype[Map[String, String]] {
  val id: ShapeId = ShapeId("smithy.test", "StringMap")
  val hints: Hints = Hints(
    smithy.api.Private(),
  )
  val underlyingSchema: Schema[Map[String, String]] = map(string, string).withId(id).addHints(hints)
  implicit val schema: Schema[StringMap] = bijection(underlyingSchema, asBijection)
}
