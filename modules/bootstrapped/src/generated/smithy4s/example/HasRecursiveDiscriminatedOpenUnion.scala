package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class HasRecursiveDiscriminatedOpenUnion(rec: RecursiveDiscriminatedOpenUnion)

object HasRecursiveDiscriminatedOpenUnion extends ShapeTag.Companion[HasRecursiveDiscriminatedOpenUnion] {
  val id: ShapeId = ShapeId("smithy4s.example", "HasRecursiveDiscriminatedOpenUnion")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(rec: RecursiveDiscriminatedOpenUnion): HasRecursiveDiscriminatedOpenUnion = HasRecursiveDiscriminatedOpenUnion(rec)

  implicit val schema: Schema[HasRecursiveDiscriminatedOpenUnion] = recursive(struct(
    RecursiveDiscriminatedOpenUnion.schema.required[HasRecursiveDiscriminatedOpenUnion]("rec", _.rec),
  )(make).withId(id).addHints(hints))
}
