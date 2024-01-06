package smithy4s.example

import _root_.java.util.UUID
import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.uuid

object ObjectKey extends Newtype[UUID] {
  val id: ShapeId = ShapeId("smithy4s.example", "ObjectKey")
  val hints: Hints = Hints(
    alloy.UuidFormat(),
  )
  val underlyingSchema: Schema[UUID] = uuid.withId(id).addHints(hints)
  implicit val schema: Schema[ObjectKey] = bijection(underlyingSchema, asBijection)
}
