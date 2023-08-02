package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.internals.StructurePatternRefinementProvider._
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object TestStructurePattern extends Newtype[TestStructurePatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestStructurePattern")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[TestStructurePatternTarget] = string.refined[TestStructurePatternTarget](alloy.StructurePattern(pattern = "{one}-{two}", target = smithy4s.ShapeId(namespace = "smithy4s.example", name = "TestStructurePatternTarget"))).withId(id).addHints(hints)
  implicit val schema: Schema[TestStructurePattern] = bijection(underlyingSchema, asBijection)
}
