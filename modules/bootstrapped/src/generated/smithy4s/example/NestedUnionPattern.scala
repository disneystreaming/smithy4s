package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.internals.StructurePatternRefinementProvider._
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object NestedUnionPattern extends Newtype[NestedUnionPatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "NestedUnionPattern")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "structurePattern"), smithy4s.Document.obj("pattern" -> smithy4s.Document.fromString("{prefix}/{tagged}"), "target" -> smithy4s.Document.fromString("smithy4s.example#NestedUnionPatternTarget"))),
  )
  val underlyingSchema: Schema[NestedUnionPatternTarget] = string.refined[NestedUnionPatternTarget](alloy.StructurePattern(pattern = "{prefix}/{tagged}", target = smithy4s.ShapeId(namespace = "smithy4s.example", name = "NestedUnionPatternTarget"))).withId(id).addHints(hints)
  implicit val schema: Schema[NestedUnionPattern] = bijection(underlyingSchema, asBijection)
}
