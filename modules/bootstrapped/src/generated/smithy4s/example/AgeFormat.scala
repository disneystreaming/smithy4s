package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class AgeFormat()

object AgeFormat extends ShapeTag.Companion[AgeFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "ageFormat")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = Some(":test(integer, member > integer)"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  ).lazily

  implicit val schema: Schema[AgeFormat] = constant(AgeFormat()).withId(id).addHints(hints)
}
