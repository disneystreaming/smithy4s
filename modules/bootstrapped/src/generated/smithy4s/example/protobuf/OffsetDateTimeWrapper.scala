package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.offsetdatetime
import smithy4s.schema.Schema.struct
import smithy4s.time.OffsetDateTime

final case class OffsetDateTimeWrapper(string: Option[OffsetDateTime] = None, compact: Option[OffsetDateTime] = None)

object OffsetDateTimeWrapper extends ShapeTag.Companion[OffsetDateTimeWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "OffsetDateTimeWrapper")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(string: Option[OffsetDateTime], compact: Option[OffsetDateTime]): OffsetDateTimeWrapper = OffsetDateTimeWrapper(string, compact)

  implicit val schema: Schema[OffsetDateTimeWrapper] = struct(
    offsetdatetime.optional[OffsetDateTimeWrapper]("string", _.string).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoOffsetDateTimeFormat"), smithy4s.Document.fromString("RFC3339_STRING"))),
    offsetdatetime.optional[OffsetDateTimeWrapper]("compact", _.compact).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoOffsetDateTimeFormat"), smithy4s.Document.fromString("PROTOBUF"))),
  )(make).withId(id).addHints(hints)
}
