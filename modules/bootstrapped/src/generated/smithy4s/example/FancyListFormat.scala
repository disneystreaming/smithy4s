package smithy4s.example

import smithy.api.Trait
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class FancyListFormat()
object FancyListFormat extends ShapeTag.Companion[FancyListFormat] {

  implicit val schema: Schema[FancyListFormat] = constant(FancyListFormat()).withId(ShapeId("smithy4s.example", "fancyListFormat"))
  .withId(ShapeId("smithy4s.example", "fancyListFormat"))
  .addHints(
    Trait(selector = Some("list:test(> member > string)"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )
}
