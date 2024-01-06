package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class NonEmptyListFormat()

object NonEmptyListFormat extends ShapeTag.Companion[NonEmptyListFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "nonEmptyListFormat")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = Some("list"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[NonEmptyListFormat] = constant(NonEmptyListFormat()).withId(id).addHints(hints)
}
