package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class AgeFormat()

object AgeFormat extends ShapeTag.Companion[AgeFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "ageFormat")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = Some(":test(integer, member > integer)"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[AgeFormat] = constant(AgeFormat()).withId(id).addHints(hints)
}
