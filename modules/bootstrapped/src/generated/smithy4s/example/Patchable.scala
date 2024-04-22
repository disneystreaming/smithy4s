package smithy4s.example

import smithy4s.Hints
import smithy4s.Nullable
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class Patchable(allowExplicitNull: Option[Nullable[Int]] = None)

object Patchable extends ShapeTag.Companion[Patchable] {
  val id: ShapeId = ShapeId("smithy4s.example", "Patchable")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(allowExplicitNull: Option[Nullable[Int]]): Patchable = Patchable(allowExplicitNull)

  implicit val schema: Schema[Patchable] = struct(
    int.nullable.optional[Patchable]("allowExplicitNull", _.allowExplicitNull),
  )(make).withId(id).addHints(hints)
}
