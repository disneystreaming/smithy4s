package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.internals.StructurePatternRefinementProvider._
import smithy4s.schema.Schema.string

object TestStructurePattern extends Newtype[TestStructurePatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestStructurePattern")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[TestStructurePatternTarget] = string.refined[TestStructurePatternTarget](alloy.StructurePattern(pattern = "{one}-{two}", target = smithy4s.ShapeId(namespace = "smithy4s.example", name = "TestStructurePatternTarget"))).withId(id).addHints(hints)
  implicit val schema: Schema[TestStructurePattern] = bijection(underlyingSchema, asBijection)
}
