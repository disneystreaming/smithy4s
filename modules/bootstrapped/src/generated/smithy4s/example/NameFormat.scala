package smithy4s.example

import smithy.api.Trait
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class NameFormat()
object NameFormat extends ShapeTag.Companion[NameFormat] {

  implicit val schema: Schema[NameFormat] = constant(NameFormat()).withId(ShapeId("smithy4s.example", "nameFormat"))
  .withId(ShapeId("smithy4s.example", "nameFormat"))
  .addHints(
    Hints(
      Trait(selector = Some("string"), structurallyExclusive = None, conflicts = None, breakingChanges = None),
    )
  )
}
