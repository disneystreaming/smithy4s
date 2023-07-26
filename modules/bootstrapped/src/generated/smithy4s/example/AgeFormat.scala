package smithy4s.example

import smithy.api.Trait
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class AgeFormat()
object AgeFormat extends ShapeTag.Companion[AgeFormat] {

  implicit val schema: Schema[AgeFormat] = constant(AgeFormat()).withId(ShapeId("smithy4s.example", "ageFormat"))
  .withId(ShapeId("smithy4s.example", "ageFormat"))
  .addHints(
    Trait(selector = Some(":test(integer, member > integer)"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )
}
