package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.internals.StructurePatternRefinementProvider._
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object NestedStructurePattern extends Newtype[NestedStructurePatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "NestedStructurePattern")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "structurePattern"), smithy4s.Document.obj("pattern" -> smithy4s.Document.fromString("{name}/{inner}"), "target" -> smithy4s.Document.fromString("smithy4s.example#NestedStructurePatternTarget"))),
  )
  val underlyingSchema: Schema[NestedStructurePatternTarget] = string.refined[NestedStructurePatternTarget](alloy.StructurePattern(pattern = "{name}/{inner}", target = smithy4s.ShapeId(namespace = "smithy4s.example", name = "NestedStructurePatternTarget"))).withId(id).addHints(hints)
  implicit val schema: Schema[NestedStructurePattern] = bijection(underlyingSchema, asBijection)
}
