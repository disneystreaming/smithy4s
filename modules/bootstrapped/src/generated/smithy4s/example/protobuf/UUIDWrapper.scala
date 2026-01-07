package smithy4s.example.protobuf

import java.util.UUID
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.uuid

final case class UUIDWrapper(uuid: Option[UUID] = None, compactUUID: Option[CompactUUID] = None)

object UUIDWrapper extends ShapeTag.Companion[UUIDWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "UUIDWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(uuid: Option[UUID], compactUUID: Option[CompactUUID]): UUIDWrapper = UUIDWrapper(uuid, compactUUID)

  implicit val schema: Schema[UUIDWrapper] = struct(
    uuid.optional[UUIDWrapper]("uuid", _.uuid),
    CompactUUID.schema.optional[UUIDWrapper]("compactUUID", _.compactUUID),
  )(make).withId(id).addHints(hints)
}
