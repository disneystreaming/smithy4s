package smithy4s.example.traits

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class Indirect0()

object Indirect0 extends ShapeTag.Companion[Indirect0] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "indirect0")

  val hints: Hints = Hints(
    ShapeId("smithy4s.example.traits", "indirect1") -> Document.obj(),
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[Indirect0] = constant(Indirect0()).withId(id).addHints(hints)
}
