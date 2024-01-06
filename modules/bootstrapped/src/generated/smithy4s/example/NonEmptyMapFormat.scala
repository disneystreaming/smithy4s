package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class NonEmptyMapFormat()

object NonEmptyMapFormat extends ShapeTag.Companion[NonEmptyMapFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "nonEmptyMapFormat")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = Some("map"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[NonEmptyMapFormat] = constant(NonEmptyMapFormat()).withId(id).addHints(hints)
}
