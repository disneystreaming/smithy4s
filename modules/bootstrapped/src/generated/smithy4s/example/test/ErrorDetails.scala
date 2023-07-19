package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class ErrorDetails(date: Timestamp, location: String)
object ErrorDetails extends ShapeTag.Companion[ErrorDetails] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "ErrorDetails")

  val hints: Hints = Hints.empty

  object Optics {
    val date = Lens[ErrorDetails, Timestamp](_.date)(n => a => a.copy(date = n))
    val location = Lens[ErrorDetails, String](_.location)(n => a => a.copy(location = n))
  }

  implicit val schema: Schema[ErrorDetails] = struct(
    timestamp.required[ErrorDetails]("date", _.date).addHints(smithy.api.TimestampFormat.EPOCH_SECONDS.widen, smithy.api.Required()),
    string.required[ErrorDetails]("location", _.location).addHints(smithy.api.Required()),
  ){
    ErrorDetails.apply
  }.withId(id).addHints(hints)
}
