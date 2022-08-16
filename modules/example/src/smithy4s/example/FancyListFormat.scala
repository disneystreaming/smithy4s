package smithy4s.example

import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant
import smithy4s.Hints

case class FancyListFormat()
object FancyListFormat extends ShapeTag.Companion[FancyListFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "fancyListFormat")

  val hints : Hints = Hints(
    smithy.api.Trait(Some("list:test(> member > string)"), None, None, None),
  )

  implicit val schema: Schema[FancyListFormat] = constant(FancyListFormat()).withId(id).addHints(hints)
}