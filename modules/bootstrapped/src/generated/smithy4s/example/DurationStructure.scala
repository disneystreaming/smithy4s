package smithy4s.example

import scala.concurrent.duration.Duration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.duration
import smithy4s.schema.Schema.struct

final case class DurationStructure(duration: Duration, duration2: MyDuration)

object DurationStructure extends ShapeTag.Companion[DurationStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "DurationStructure")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(duration: Duration, duration2: MyDuration): DurationStructure = DurationStructure(duration, duration2)

  implicit val schema: Schema[DurationStructure] = struct(
    duration.required[DurationStructure]("duration", _.duration),
    MyDuration.schema.required[DurationStructure]("duration2", _.duration2),
  )(make).withId(id).addHints(hints)
}
