package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Timestamp
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp

final case class TimestampOperationInput(httpDate: Timestamp = Timestamp(1716459630L, 0), epochSeconds: Timestamp = Timestamp(1716459630L, 0), dateTime: Timestamp = Timestamp(1716459630L, 0))

object TimestampOperationInput extends ShapeTag.Companion[TimestampOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "TimestampOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(httpDate: Timestamp, epochSeconds: Timestamp, dateTime: Timestamp): TimestampOperationInput = TimestampOperationInput(httpDate, epochSeconds, dateTime)

  implicit val schema: Schema[TimestampOperationInput] = struct(
    timestamp.required[TimestampOperationInput]("httpDate", _.httpDate).addHints(smithy.api.Default(smithy4s.Document.fromString("Thu, 23 May 2024 10:20:30 GMT")), smithy.api.TimestampFormat.HTTP_DATE.widen),
    timestamp.required[TimestampOperationInput]("epochSeconds", _.epochSeconds).addHints(smithy.api.Default(smithy4s.Document.fromDouble(1.71645963E9d)), smithy.api.TimestampFormat.EPOCH_SECONDS.widen),
    timestamp.required[TimestampOperationInput]("dateTime", _.dateTime).addHints(smithy.api.Default(smithy4s.Document.fromString("2024-05-23T10:20:30.000Z")), smithy.api.TimestampFormat.DATE_TIME.widen),
  )(make).withId(id).addHints(hints)
}
