package smithy4s.example.protobuf

import alloy.LocalDate
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class LocalDateWrapper(localDate: Option[LocalDate] = None, compactLocalDate: Option[LocalDate] = None)

object LocalDateWrapper extends ShapeTag.Companion[LocalDateWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "LocalDateWrapper")

  val hints: Hints = Hints(
    alloy.proto.ProtoEnabled(),
  ).lazily

  // constructor using the original order from the spec
  private def make(localDate: Option[LocalDate], compactLocalDate: Option[LocalDate]): LocalDateWrapper = LocalDateWrapper(localDate, compactLocalDate)

  implicit val schema: Schema[LocalDateWrapper] = struct(
    LocalDate.schema.optional[LocalDateWrapper]("localDate", _.localDate),
    LocalDate.schema.optional[LocalDateWrapper]("compactLocalDate", _.compactLocalDate).addHints(alloy.proto.ProtoCompactLocalDate()),
  )(make).withId(id).addHints(hints)
}
