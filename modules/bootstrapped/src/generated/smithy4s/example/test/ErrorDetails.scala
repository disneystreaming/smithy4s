package smithy4s.example.test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.Timestamp
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.timestamp

final case class ErrorDetails(date: Timestamp, location: String)

object ErrorDetails extends ShapeTag.Companion[ErrorDetails] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "ErrorDetails")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[ErrorDetails] = struct(
    timestamp.required[ErrorDetails]("date", _.date).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen),
    string.required[ErrorDetails]("location", _.location),
  ){
    ErrorDetails.apply
  }.withId(id).addHints(hints)
}
