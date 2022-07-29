package smithy4s.example

import smithy4s.schema.Schema._

case class FancyListFormat()
object FancyListFormat extends smithy4s.ShapeTag.Companion[FancyListFormat] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "fancyListFormat")

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Trait(Some("list:test(> member > string)"), None, None),
  )

  implicit val schema: smithy4s.Schema[FancyListFormat] = constant(FancyListFormat()).withId(id).addHints(hints)
}