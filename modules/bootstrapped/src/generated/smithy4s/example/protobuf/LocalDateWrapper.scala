package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.localdate
import smithy4s.schema.Schema.struct
import smithy4s.time.LocalDate

final case class LocalDateWrapper(localDate: Option[LocalDate] = None, compactLocalDate: Option[LocalDate] = None)

object LocalDateWrapper extends ShapeTag.Companion[LocalDateWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "LocalDateWrapper")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(localDate: Option[LocalDate], compactLocalDate: Option[LocalDate]): LocalDateWrapper = LocalDateWrapper(localDate, compactLocalDate)

  implicit val schema: Schema[LocalDateWrapper] = struct(
    localdate.optional[LocalDateWrapper]("localDate", _.localDate),
    localdate.optional[LocalDateWrapper]("compactLocalDate", _.compactLocalDate).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoCompactLocalDate"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
