package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.constant

final case class Hash()

object Hash extends ShapeTag.Companion[Hash] {
  val id: ShapeId = ShapeId("smithy4s.example", "hash")

  val hints: Hints = Hints(
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[Hash] = constant(Hash()).withId(id).addHints(hints)
}
