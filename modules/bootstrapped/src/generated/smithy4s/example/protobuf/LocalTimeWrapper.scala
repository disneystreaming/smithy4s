package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.localtime
import smithy4s.schema.Schema.struct
import smithy4s.time.LocalTime

final case class LocalTimeWrapper(localTime: Option[LocalTime] = None, compactLocalTime: Option[LocalTime] = None)

object LocalTimeWrapper extends ShapeTag.Companion[LocalTimeWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "LocalTimeWrapper")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(localTime: Option[LocalTime], compactLocalTime: Option[LocalTime]): LocalTimeWrapper = LocalTimeWrapper(localTime, compactLocalTime)

  implicit val schema: Schema[LocalTimeWrapper] = struct(
    localtime.optional[LocalTimeWrapper]("localTime", _.localTime),
    localtime.optional[LocalTimeWrapper]("compactLocalTime", _.compactLocalTime).addHints(Hints.dynamic(ShapeId("alloy.proto", "protoCompactLocalTime"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
