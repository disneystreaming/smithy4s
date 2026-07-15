package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NestedStructurePatternTarget(name: String, inner: TestStructurePattern)

object NestedStructurePatternTarget extends ShapeTag.Companion[NestedStructurePatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "NestedStructurePatternTarget")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(name: String, inner: TestStructurePattern): NestedStructurePatternTarget = NestedStructurePatternTarget(name, inner)

  implicit val schema: Schema[NestedStructurePatternTarget] = struct[NestedStructurePatternTarget](
    string.required[NestedStructurePatternTarget]("name", _.name),
    TestStructurePattern.schema.required[NestedStructurePatternTarget]("inner", _.inner),
  )(make).withId(id).addHints(hints)
}
