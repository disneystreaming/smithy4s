package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class GetCurrentTimeOutput(time: Timestamp)

object GetCurrentTimeOutput extends ShapeTag.Companion[GetCurrentTimeOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetCurrentTimeOutput")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(time: Timestamp): GetCurrentTimeOutput = GetCurrentTimeOutput(time)

  implicit val schema: Schema[GetCurrentTimeOutput] = struct(
    timestamp.required[GetCurrentTimeOutput]("time", _.time),
  )(make).withId(id).addHints(hints)
}
