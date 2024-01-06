package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string

final case class TestStructurePatternTarget(one: String, two: Int)

object TestStructurePatternTarget extends ShapeTag.Companion[TestStructurePatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestStructurePatternTarget")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[TestStructurePatternTarget] = struct(
    string.required[TestStructurePatternTarget]("one", _.one),
    int.required[TestStructurePatternTarget]("two", _.two),
  ){
    TestStructurePatternTarget.apply
  }.withId(id).addHints(hints)
}
