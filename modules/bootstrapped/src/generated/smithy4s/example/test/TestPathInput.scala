package smithy4s.example.test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class TestPathInput(path: String)

object TestPathInput extends ShapeTag.Companion[TestPathInput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "TestPathInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[TestPathInput] = struct(
    string.required[TestPathInput]("path", _.path).addHints(smithy.api.HttpLabel()),
  ){
    TestPathInput.apply
  }.withId(id).addHints(hints)
}
