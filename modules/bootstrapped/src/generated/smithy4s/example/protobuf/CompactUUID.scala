package smithy4s.example.protobuf

import java.util.UUID
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.uuid

object CompactUUID extends Newtype[UUID] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "CompactUUID")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "uuidFormat"), smithy4s.Document.obj()),
    Hints.dynamic(ShapeId("alloy.proto", "protoCompactUUID"), smithy4s.Document.obj()),
  )
  val underlyingSchema: Schema[UUID] = uuid.withId(id).addHints(hints)
  implicit val schema: Schema[CompactUUID] = bijection(underlyingSchema, asBijection)
}
