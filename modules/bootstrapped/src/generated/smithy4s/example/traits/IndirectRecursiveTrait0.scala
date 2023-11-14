package smithy4s.example.traits

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** A trait with an indirect recursive reference */
final case class IndirectRecursiveTrait0()

object IndirectRecursiveTrait0 extends ShapeTag.Companion[IndirectRecursiveTrait0] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "indirectRecursiveTrait0")

  val hints: Hints = Hints(
    ShapeId("smithy4s.example.traits", "indirectRecursiveTrait1") -> Document.obj(),
    smithy.api.Documentation("A trait with an indirect recursive reference"),
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[IndirectRecursiveTrait0] = constant(IndirectRecursiveTrait0()).withId(id).addHints(hints)
}
