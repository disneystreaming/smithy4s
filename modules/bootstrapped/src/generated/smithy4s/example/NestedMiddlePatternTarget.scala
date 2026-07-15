package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class NestedMiddlePatternTarget(id: Int, choice: NestedInnerUnionPattern)

object NestedMiddlePatternTarget extends ShapeTag.Companion[NestedMiddlePatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "NestedMiddlePatternTarget")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(id: Int, choice: NestedInnerUnionPattern): NestedMiddlePatternTarget = NestedMiddlePatternTarget(id, choice)

  implicit val schema: Schema[NestedMiddlePatternTarget] = struct[NestedMiddlePatternTarget](
    int.required[NestedMiddlePatternTarget]("id", _.id),
    NestedInnerUnionPattern.schema.required[NestedMiddlePatternTarget]("choice", _.choice),
  )(make).withId(id).addHints(hints)
}
