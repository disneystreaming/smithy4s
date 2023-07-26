package smithy4s.example.test

import smithy.api.Required
import smithy.api.TimestampFormat
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class ErrorDetails(date: Timestamp, location: String)
object ErrorDetails extends ShapeTag.$Companion[ErrorDetails] {
  val $id: ShapeId = ShapeId("smithy4s.example.test", "ErrorDetails")

  val $hints: Hints = Hints.empty

  val date: FieldLens[ErrorDetails, Timestamp] = timestamp.required[ErrorDetails]("date", _.date, n => c => c.copy(date = n)).addHints(TimestampFormat.EPOCH_SECONDS.widen, Required())
  val location: FieldLens[ErrorDetails, String] = string.required[ErrorDetails]("location", _.location, n => c => c.copy(location = n)).addHints(Required())

  implicit val $schema: Schema[ErrorDetails] = struct(
    date,
    location,
  ){
    ErrorDetails.apply
  }.withId($id).addHints($hints)
}
