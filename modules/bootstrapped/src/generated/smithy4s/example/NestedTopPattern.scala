package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.internals.StructurePatternRefinementProvider._
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object NestedTopPattern extends Newtype[NestedTopPatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "NestedTopPattern")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("alloy", "structurePattern"), smithy4s.Document.obj("pattern" -> smithy4s.Document.fromString("{tenant}/{resource}"), "target" -> smithy4s.Document.fromString("smithy4s.example#NestedTopPatternTarget"))),
  )
  val underlyingSchema: Schema[NestedTopPatternTarget] = string.refined[NestedTopPatternTarget](alloy.StructurePattern(pattern = "{tenant}/{resource}", target = smithy4s.ShapeId(namespace = "smithy4s.example", name = "NestedTopPatternTarget"))).withId(id).addHints(hints)
  implicit val schema: Schema[NestedTopPattern] = bijection(underlyingSchema, asBijection)
}
