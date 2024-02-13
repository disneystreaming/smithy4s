package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class NameFormat()

object NameFormat extends ShapeTag.Companion[NameFormat] {
  val id: ShapeId = ShapeId("smithy4s.example", "nameFormat")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = Some("string"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
  ).lazily

  implicit val schema: Schema[NameFormat] = constant(NameFormat()).withId(id).addHints(hints)
}
