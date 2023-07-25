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
  val hints: Hints = Hints.empty

  val time = timestamp.required[GetCurrentTimeOutput]("time", _.time).addHints(smithy.api.Required())

  implicit val schema: Schema[GetCurrentTimeOutput] = struct(
    time,
  ){
    GetCurrentTimeOutput.apply
  }.withId(ShapeId("smithy4s.example", "GetCurrentTimeOutput")).addHints(hints)
}
