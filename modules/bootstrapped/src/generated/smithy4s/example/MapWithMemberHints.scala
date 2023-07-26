package smithy4s.example

import smithy.api.Deprecated
import smithy.api.Documentation
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

/** @param key
  *   mapFoo
  * @param value
  *   mapBar
  */
object MapWithMemberHints extends Newtype[Map[String, Int]] {
  val underlyingSchema: Schema[Map[String, Int]] = map(string.addMemberHints(Documentation("mapFoo")), int.addMemberHints(Documentation("mapBar"), Deprecated(message = None, since = None)))
  .withId(ShapeId("smithy4s.example", "MapWithMemberHints"))
  .addHints()

  implicit val schema: Schema[MapWithMemberHints] = bijection(underlyingSchema, asBijection)
}
