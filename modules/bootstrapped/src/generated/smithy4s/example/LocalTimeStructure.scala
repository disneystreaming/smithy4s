package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.localtime
import smithy4s.schema.Schema.struct
import smithy4s.time.LocalTime

final case class LocalTimeStructure(localTime: LocalTime, localTime2: MyLocalTime)

object LocalTimeStructure extends ShapeTag.Companion[LocalTimeStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "LocalTimeStructure")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(localTime: LocalTime, localTime2: MyLocalTime): LocalTimeStructure = LocalTimeStructure(localTime, localTime2)

  implicit val schema: Schema[LocalTimeStructure] = struct(
    localtime.required[LocalTimeStructure]("localTime", _.localTime),
    MyLocalTime.schema.required[LocalTimeStructure]("localTime2", _.localTime2),
  )(make).withId(id).addHints(hints)
}
