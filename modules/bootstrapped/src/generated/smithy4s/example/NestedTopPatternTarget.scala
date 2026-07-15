package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NestedTopPatternTarget(tenant: String, resource: NestedMiddlePattern)

object NestedTopPatternTarget extends ShapeTag.Companion[NestedTopPatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "NestedTopPatternTarget")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(tenant: String, resource: NestedMiddlePattern): NestedTopPatternTarget = NestedTopPatternTarget(tenant, resource)

  implicit val schema: Schema[NestedTopPatternTarget] = struct[NestedTopPatternTarget](
    string.required[NestedTopPatternTarget]("tenant", _.tenant),
    NestedMiddlePattern.schema.required[NestedTopPatternTarget]("resource", _.resource),
  )(make).withId(id).addHints(hints)
}
