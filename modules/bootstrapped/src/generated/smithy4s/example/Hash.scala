package smithy4s.example

import smithy.api.Trait
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class Hash()
object Hash extends ShapeTag.Companion[Hash] {

  implicit val schema: Schema[Hash] = constant(Hash()).withId(ShapeId("smithy4s.example", "hash"))
  .withId(ShapeId("smithy4s.example", "hash"))
  .addHints(
    Hints(
      Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
    )
  )
}
