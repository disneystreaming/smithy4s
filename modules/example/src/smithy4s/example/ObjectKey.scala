package smithy4s.example

import java.util.UUID
import smithy4s.Newtype
import smithy4s.schema.Schema._

object ObjectKey extends Newtype[UUID] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "ObjectKey")
  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy4s.api.UuidFormat(),
  )
  val underlyingSchema : smithy4s.Schema[UUID] = uuid.withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[ObjectKey] = bijection(underlyingSchema, asBijection)
}