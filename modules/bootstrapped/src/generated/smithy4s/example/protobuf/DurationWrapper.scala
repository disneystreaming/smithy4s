package smithy4s.example.protobuf

import scala.concurrent.duration.Duration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.duration
import smithy4s.schema.Schema.struct

final case class DurationWrapper(duration: Option[Duration] = None)

object DurationWrapper extends ShapeTag.Companion[DurationWrapper] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "DurationWrapper")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy.proto", "protoEnabled"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(duration: Option[Duration]): DurationWrapper = DurationWrapper(duration)

  implicit val schema: Schema[DurationWrapper] = struct(
    duration.optional[DurationWrapper]("duration", _.duration),
  )(make).withId(id).addHints(hints)
}
