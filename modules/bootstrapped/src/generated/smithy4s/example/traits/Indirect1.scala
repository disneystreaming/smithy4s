package smithy4s.example.traits

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class Indirect1()

object Indirect1 extends ShapeTag.Companion[Indirect1] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "indirect1")

  val hints: Hints = Hints(
    ShapeId("smithy4s.example.traits", "indirect2") -> Document.obj(),
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[Indirect1] = constant(Indirect1()).withId(id).addHints(hints)
}
