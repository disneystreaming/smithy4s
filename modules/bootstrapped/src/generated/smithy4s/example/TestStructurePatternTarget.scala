package smithy4s.example

import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestStructurePatternTarget(one: String, two: Int)
object TestStructurePatternTarget extends ShapeTag.$Companion[TestStructurePatternTarget] {
  val $id: ShapeId = ShapeId("smithy4s.example", "TestStructurePatternTarget")

  val $hints: Hints = Hints.empty

  val one: FieldLens[TestStructurePatternTarget, String] = string.required[TestStructurePatternTarget]("one", _.one, n => c => c.copy(one = n)).addHints(Required())
  val two: FieldLens[TestStructurePatternTarget, Int] = int.required[TestStructurePatternTarget]("two", _.two, n => c => c.copy(two = n)).addHints(Required())

  implicit val $schema: Schema[TestStructurePatternTarget] = struct(
    one,
    two,
  ){
    TestStructurePatternTarget.apply
  }.withId($id).addHints($hints)
}
