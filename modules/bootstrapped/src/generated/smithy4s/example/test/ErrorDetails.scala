package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class ErrorDetails(date: Timestamp, location: String)

object ErrorDetails extends ShapeTag.Companion[ErrorDetails] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "ErrorDetails")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(date: Timestamp, location: String): ErrorDetails = ErrorDetails(date, location)

  implicit val schema: Schema[ErrorDetails] = struct(
    timestamp.required[ErrorDetails]("date", _.date).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen),
    string.required[ErrorDetails]("location", _.location),
  )(make).withId(id).addHints(hints)
}
