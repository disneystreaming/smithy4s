package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.internals.StructurePatternRefinementProvider._
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object TestUnionPattern extends Newtype[TestUnionPatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestUnionPattern")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "structurePattern"), smithy4s.Document.obj("pattern" -> smithy4s.Document.fromString("{label}:{value}"), "target" -> smithy4s.Document.fromString("smithy4s.example#TestUnionPatternTarget"))),
  )
  val underlyingSchema: Schema[TestUnionPatternTarget] = string.refined[TestUnionPatternTarget](alloy.StructurePattern(pattern = "{label}:{value}", target = smithy4s.ShapeId(namespace = "smithy4s.example", name = "TestUnionPatternTarget"))).withId(id).addHints(hints)
  implicit val schema: Schema[TestUnionPattern] = bijection(underlyingSchema, asBijection)
}
