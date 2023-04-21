package smithy4s.example

import java.util.UUID
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.uuid

object ObjectKey extends Newtype[UUID] {
  val id: ShapeId = ShapeId("smithy4s.example", "ObjectKey")
  val hints: Hints = Hints(
    alloy.UuidFormat(),
  )
  val underlyingSchema: Schema[UUID] = uuid.withId(id).addHints(hints)
  implicit val schema: Schema[ObjectKey] = bijection(underlyingSchema, asBijection)
}
