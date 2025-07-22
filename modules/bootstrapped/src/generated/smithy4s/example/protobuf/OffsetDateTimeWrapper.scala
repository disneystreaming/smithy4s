package smithy4s.example.protobuf

import alloy.OffsetDateTime
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class OffsetDateTimeWrapper(string: Option[OffsetDateTime] = None, compact: Option[OffsetDateTime] = None)

object OffsetDateTimeWrapper extends ShapeTag.Companion[OffsetDateTimeWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "OffsetDateTimeWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(string: Option[OffsetDateTime], compact: Option[OffsetDateTime]): OffsetDateTimeWrapper = OffsetDateTimeWrapper(string, compact)

  implicit val schema: Schema[OffsetDateTimeWrapper] = struct(
    OffsetDateTime.schema.optional[OffsetDateTimeWrapper]("string", _.string).addHints(alloy.proto.ProtoOffsetDateTimeFormat.RFC3339_STRING.widen),
    OffsetDateTime.schema.optional[OffsetDateTimeWrapper]("compact", _.compact).addHints(alloy.proto.ProtoOffsetDateTimeFormat.PROTOBUF.widen),
  )(make).withId(id).addHints(hints)
}
