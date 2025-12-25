package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.offsetdatetime
import smithy4s.schema.Schema.struct
import smithy4s.time.OffsetDateTime

final case class OffsetDateTimeStructure(offsetDateTime: OffsetDateTime, offsetDateTime2: MyOffsetDateTime)

object OffsetDateTimeStructure extends ShapeTag.Companion[OffsetDateTimeStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "OffsetDateTimeStructure")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(offsetDateTime: OffsetDateTime, offsetDateTime2: MyOffsetDateTime): OffsetDateTimeStructure = OffsetDateTimeStructure(offsetDateTime, offsetDateTime2)

  implicit val schema: Schema[OffsetDateTimeStructure] = struct(
    offsetdatetime.required[OffsetDateTimeStructure]("offsetDateTime", _.offsetDateTime),
    MyOffsetDateTime.schema.required[OffsetDateTimeStructure]("offsetDateTime2", _.offsetDateTime2),
  )(make).withId(id).addHints(hints)
}
