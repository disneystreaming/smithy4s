package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestPathInput(path: String)

object TestPathInput extends ShapeTag.Companion[TestPathInput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "TestPathInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  implicit val schema: Schema[TestPathInput] = struct(
    string.required[TestPathInput]("path", _.path).addHints(smithy.api.HttpLabel()),
  ){
    TestPathInput.apply
  }.withId(id).addHints(hints)
}
