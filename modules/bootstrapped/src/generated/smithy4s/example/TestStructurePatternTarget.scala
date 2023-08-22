package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestStructurePatternTarget(one: String, two: Int)
object TestStructurePatternTarget extends ShapeTag.Companion[TestStructurePatternTarget] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestStructurePatternTarget")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[TestStructurePatternTarget] = struct(
    string.required[TestStructurePatternTarget]("one", _.one).addHints(smithy.api.Required()),
    int.required[TestStructurePatternTarget]("two", _.two).addHints(smithy.api.Required()),
  ){
    TestStructurePatternTarget.apply
  }.withId(id).addHints(hints)
}
