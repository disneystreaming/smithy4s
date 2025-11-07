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

  // constructor using the original order from the spec
  private def make(key: String): TestDiscriminatedInput = TestDiscriminatedInput(key)

  implicit val schema: Schema[TestDiscriminatedInput] = struct(
    string.required[TestDiscriminatedInput]("key", _.key).addHints(Hints.dynamic(ShapeId("smithy.api", "httpLabel"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
