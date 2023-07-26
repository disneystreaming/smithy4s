package smithy4s.example

import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class GetCurrentTimeOutput(time: Timestamp)
object GetCurrentTimeOutput extends ShapeTag.Companion[GetCurrentTimeOutput] {

  val time: FieldLens[GetCurrentTimeOutput, Timestamp] = timestamp.required[GetCurrentTimeOutput]("time", _.time, n => c => c.copy(time = n)).addHints(Required())

  implicit val schema: Schema[GetCurrentTimeOutput] = struct(
    time,
  ){
    GetCurrentTimeOutput.apply
  }
  .withId(ShapeId("smithy4s.example", "GetCurrentTimeOutput"))
}
