package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object TestIdRefValueMap extends Newtype[Map[String, ShapeId]] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestIdRefValueMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, ShapeId]] = map(string, string.refined[ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)).addMemberHints(Hints.dynamic(ShapeId("smithy.api", "idRef"), smithy4s.Document.obj()))).withId(id).addHints(hints)
  implicit val schema: Schema[TestIdRefValueMap] = bijection(underlyingSchema, asBijection)
}
