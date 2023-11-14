package smithy4s.example.traits

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class IndirectRecursiveTrait1()

object IndirectRecursiveTrait1 extends ShapeTag.Companion[IndirectRecursiveTrait1] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "indirectRecursiveTrait1")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
    ShapeId("smithy4s.example.traits", "indirectRecursiveTrait0") -> Document.obj(),
  )

  implicit val schema: Schema[IndirectRecursiveTrait1] = constant(IndirectRecursiveTrait1()).withId(id).addHints(hints)
}
