package smithy4s.example

import smithy.api.Trait
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class NonEmptyMapFormat()
object NonEmptyMapFormat extends ShapeTag.Companion[NonEmptyMapFormat] {

  implicit val schema: Schema[NonEmptyMapFormat] = constant(NonEmptyMapFormat()).withId(ShapeId("smithy4s.example", "nonEmptyMapFormat"))
  .withId(ShapeId("smithy4s.example", "nonEmptyMapFormat"))
  .addHints(
    Hints(
      Trait(selector = Some("map"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
    )
  )
}
