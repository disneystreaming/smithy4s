package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NestedUnionPatternTarget(prefix: String, tagged: TestUnionPattern)

object NestedUnionPatternTarget extends ShapeTag.Companion[NestedUnionPatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "NestedUnionPatternTarget")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(prefix: String, tagged: TestUnionPattern): NestedUnionPatternTarget = NestedUnionPatternTarget(prefix, tagged)

  implicit val schema: Schema[NestedUnionPatternTarget] = struct[NestedUnionPatternTarget](
    string.required[NestedUnionPatternTarget]("prefix", _.prefix),
    TestUnionPattern.schema.required[NestedUnionPatternTarget]("tagged", _.tagged),
  )(make).withId(id).addHints(hints)
}
