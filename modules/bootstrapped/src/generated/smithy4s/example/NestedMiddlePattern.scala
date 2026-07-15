package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.internals.StructurePatternRefinementProvider._
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object NestedMiddlePattern extends Newtype[NestedMiddlePatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "NestedMiddlePattern")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "structurePattern"), smithy4s.Document.obj("pattern" -> smithy4s.Document.fromString("{id}|{choice}"), "target" -> smithy4s.Document.fromString("smithy4s.example#NestedMiddlePatternTarget"))),
  )
  val underlyingSchema: Schema[NestedMiddlePatternTarget] = string.refined[NestedMiddlePatternTarget](alloy.StructurePattern(pattern = "{id}|{choice}", target = smithy4s.ShapeId(namespace = "smithy4s.example", name = "NestedMiddlePatternTarget"))).withId(id).addHints(hints)
  implicit val schema: Schema[NestedMiddlePattern] = bijection(underlyingSchema, asBijection)
}
