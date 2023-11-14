package smithy4s.example.traits

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** A trait with no recursive references to itself */
final case class NonRecursiveTrait()

object NonRecursiveTrait extends ShapeTag.Companion[NonRecursiveTrait] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "nonRecursiveTrait")

  val hints: Hints = Hints(
    smithy.api.Documentation("A trait with no recursive references to itself"),
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[NonRecursiveTrait] = constant(NonRecursiveTrait()).withId(id).addHints(hints)
}
