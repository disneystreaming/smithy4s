package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.timestamp
import smithy4s.time.Timestamp

final case class TimestampOperationInput(httpDate: Timestamp = Timestamp(1716459630L, 0), epochSeconds: Timestamp = Timestamp(1716459630L, 0), dateTime: Timestamp = Timestamp(1716459630L, 0))

object TimestampOperationInput extends ShapeTag.Companion[TimestampOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "TimestampOperationInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(httpDate: Timestamp, epochSeconds: Timestamp, dateTime: Timestamp): TimestampOperationInput = TimestampOperationInput(httpDate, epochSeconds, dateTime)

  implicit val schema: Schema[TimestampOperationInput] = struct(
    timestamp.required[TimestampOperationInput]("httpDate", _.httpDate).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromString("Thu, 23 May 2024 10:20:30 GMT")), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("http-date"))),
    timestamp.required[TimestampOperationInput]("epochSeconds", _.epochSeconds).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromLong(1716459630)), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("epoch-seconds"))),
    timestamp.required[TimestampOperationInput]("dateTime", _.dateTime).addHints(Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromString("2024-05-23T10:20:30.000Z")), Hints.dynamic(ShapeId("smithy.api", "timestampFormat"), smithy4s.Document.fromString("date-time"))),
  )(make).withId(id).addHints(hints)
}
