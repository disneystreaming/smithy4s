package smithy4s.example

import alloy.StructurePattern
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.internals.StructurePatternRefinementProvider._
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object TestStructurePattern extends Newtype[TestStructurePatternTarget] {
  val underlyingSchema: Schema[TestStructurePatternTarget] = string.refined[TestStructurePatternTarget](StructurePattern(pattern = "{one}-{two}", target = "smithy4s.example#TestStructurePatternTarget"))
  .withId(ShapeId("smithy4s.example", "TestStructurePattern"))

  implicit val schema: Schema[TestStructurePattern] = bijection(underlyingSchema, asBijection)
}
