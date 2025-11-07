package smithy4s.example

import smithy4s.Hints
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
  val id: ShapeId = ShapeId("smithy4s.example", "MapWithMemberHints")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, Int]] = map(string.addMemberHints(Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("mapFoo"))), int.addMemberHints(Hints.dynamic(ShapeId("smithy.api", "deprecated"), smithy4s.Document.obj()), Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("mapBar")))).withId(id).addHints(hints)
  implicit val schema: Schema[MapWithMemberHints] = bijection(underlyingSchema, asBijection)
}
