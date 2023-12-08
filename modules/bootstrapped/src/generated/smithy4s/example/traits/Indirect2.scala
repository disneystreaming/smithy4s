package smithy4s.example.traits

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class Indirect2()

object Indirect2 extends ShapeTag.Companion[Indirect2] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "indirect2")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
    ShapeId("smithy4s.example.traits", "indirect0") -> Document.obj(),
  )

  implicit val schema: Schema[Indirect2] = constant(Indirect2()).withId(id).addHints(hints)
}
