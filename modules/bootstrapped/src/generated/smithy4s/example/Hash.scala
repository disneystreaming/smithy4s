package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class Hash()

object Hash extends ShapeTag.Companion[Hash] {
  val id: ShapeId = ShapeId("smithy4s.example", "hash")

  val hints: Hints = Hints.lazily(
    Hints(
      smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
    )
  )

  implicit val schema: Schema[Hash] = constant(Hash()).withId(id).addHints(hints)
}
