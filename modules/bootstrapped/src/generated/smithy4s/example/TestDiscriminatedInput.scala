package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class TestDiscriminatedInput(key: String)

object TestDiscriminatedInput extends ShapeTag.Companion[TestDiscriminatedInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "TestDiscriminatedInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[TestDiscriminatedInput] = struct(
    string.required[TestDiscriminatedInput]("key", _.key).addHints(smithy.api.HttpLabel()),
  ){
    TestDiscriminatedInput.apply
  }.withId(id).addHints(hints)
}
