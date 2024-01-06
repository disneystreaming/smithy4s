package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class NameFormat()

object NameFormat extends ShapeTag.Companion[NameFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "nameFormat")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = Some("string"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[NameFormat] = constant(NameFormat()).withId(id).addHints(hints)
}
