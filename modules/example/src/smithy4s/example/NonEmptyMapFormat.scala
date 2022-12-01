package smithy4s.example

import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant
import smithy4s.Hints

case class NonEmptyMapFormat()
object NonEmptyMapFormat extends ShapeTag.Companion[NonEmptyMapFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "nonEmptyMapFormat")

  val hints : Hints = Hints(
    smithy.api.Trait(selector = Some("map"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[NonEmptyMapFormat] = constant(NonEmptyMapFormat()).withId(id).addHints(hints)
}