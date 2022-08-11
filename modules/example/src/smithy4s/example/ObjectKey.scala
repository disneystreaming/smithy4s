package smithy4s.example

import smithy4s._
import java.util.UUID
import smithy4s.schema.Schema._

object ObjectKey extends Newtype[UUID] {
  val id: ShapeId = ShapeId("smithy4s.example", "ObjectKey")
  val hints : Hints = Hints(
    smithy4s.api.UuidFormat(),
  )
  val underlyingSchema : Schema[UUID] = uuid.withId(id).addHints(hints)
  implicit val schema : Schema[ObjectKey] = bijection(underlyingSchema, asBijection)
}