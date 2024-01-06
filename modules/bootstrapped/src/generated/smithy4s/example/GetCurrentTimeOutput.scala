package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Timestamp
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class GetCurrentTimeOutput(time: Timestamp)

object GetCurrentTimeOutput extends ShapeTag.Companion[GetCurrentTimeOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetCurrentTimeOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetCurrentTimeOutput] = struct(
    timestamp.required[GetCurrentTimeOutput]("time", _.time),
  ){
    GetCurrentTimeOutput.apply
  }.withId(id).addHints(hints)
}
