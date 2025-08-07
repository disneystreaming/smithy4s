package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.localdate
import smithy4s.schema.Schema.struct
import smithy4s.time.LocalDate

final case class LocalDateStructure(localDate: LocalDate, localDate2: MyLocalDate)

object LocalDateStructure extends ShapeTag.Companion[LocalDateStructure] {
  val id: ShapeId = ShapeId("smithy4s.example", "LocalDateStructure")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(localDate: LocalDate, localDate2: MyLocalDate): LocalDateStructure = LocalDateStructure(localDate, localDate2)

  implicit val schema: Schema[LocalDateStructure] = struct(
    localdate.required[LocalDateStructure]("localDate", _.localDate),
    MyLocalDate.schema.required[LocalDateStructure]("localDate2", _.localDate2),
  )(make).withId(id).addHints(hints)
}
