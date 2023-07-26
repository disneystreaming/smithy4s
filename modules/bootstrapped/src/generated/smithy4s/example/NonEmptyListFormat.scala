package smithy4s.example

import smithy.api.Trait
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class NonEmptyListFormat()
object NonEmptyListFormat extends ShapeTag.Companion[NonEmptyListFormat] {

  implicit val schema: Schema[NonEmptyListFormat] = constant(NonEmptyListFormat()).withId(ShapeId("smithy4s.example", "nonEmptyListFormat"))
  .withId(ShapeId("smithy4s.example", "nonEmptyListFormat"))
  .addHints(
    Trait(selector = Some("list"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )
}
