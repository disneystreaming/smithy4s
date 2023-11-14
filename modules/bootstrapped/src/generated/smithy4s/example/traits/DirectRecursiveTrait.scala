package smithy4s.example.traits

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

/** A trait with a direct recursive reference */
final case class DirectRecursiveTrait()

object DirectRecursiveTrait extends ShapeTag.Companion[DirectRecursiveTrait] {
  val id: ShapeId = ShapeId("smithy4s.example.traits", "directRecursiveTrait")

  val hints: Hints = Hints(
    smithy.api.Documentation("A trait with a direct recursive reference"),
    ShapeId("smithy4s.example.traits", "directRecursiveTrait") -> Document.obj(),
    smithy.api.Trait(selector = None, structurallyExclusive = None, conflicts = None, breakingChanges = None),
  )

  implicit val schema: Schema[DirectRecursiveTrait] = constant(DirectRecursiveTrait()).withId(id).addHints(hints)
}
