package smithy4s.example.traits

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** A trait with no recursion in itself, referencing recursive traits */
final case class NonRecursiveTraitReferencingOthers()

object NonRecursiveTraitReferencingOthers extends ShapeTag.Companion[NonRecursiveTraitReferencingOthers] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "nonRecursiveTraitReferencingOthers")

  val hints: Hints = Hints(
    smithy4s.example.traits.IndirectRecursiveTrait1(),
    smithy4s.example.traits.IndirectRecursiveTrait0(),
    smithy4s.example.traits.DirectRecursiveTrait(),
    smithy4s.example.traits.NonRecursiveTrait(),
    smithy.api.Documentation("A trait with no recursion in itself, referencing recursive traits"),
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[NonRecursiveTraitReferencingOthers] = constant(NonRecursiveTraitReferencingOthers()).withId(id).addHints(hints)
}
