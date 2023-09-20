package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

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
