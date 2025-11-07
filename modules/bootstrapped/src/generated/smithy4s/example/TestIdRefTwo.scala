package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object TestIdRefTwo extends Newtype[ShapeId] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestIdRefTwo")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "idRef"), smithy4s.Document.obj()),
  )
  val underlyingSchema: Schema[ShapeId] = string.refined[ShapeId](smithy.api.IdRef(selector = "*", failWhenMissing = None, errorMessage = None)).withId(id).addHints(hints)
  implicit val schema: Schema[TestIdRefTwo] = bijection(underlyingSchema, asBijection)
}
