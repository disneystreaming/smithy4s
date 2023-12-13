package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class FancyListFormat()

object FancyListFormat extends ShapeTag.Companion[FancyListFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "fancyListFormat")

  val hints: Hints = Hints.lazily(
    Hints(
      smithy.api.Trait(selector = Some("list:test(> member > string)"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
    )
  )

  implicit val schema: Schema[FancyListFormat] = constant(FancyListFormat()).withId(id).addHints(hints)
}
