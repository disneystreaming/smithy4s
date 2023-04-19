package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

case class Eq()
object Eq extends ShapeTag.Companion[Eq] {
  val id: ShapeId = ShapeId("smithy4s.example", "eq")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[Eq] = constant(Eq()).withId(id).addHints(hints)
}