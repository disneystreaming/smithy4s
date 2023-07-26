package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestDiscriminatedInput(key: String)
object TestDiscriminatedInput extends ShapeTag.Companion[TestDiscriminatedInput] {

  val key = string.required[TestDiscriminatedInput]("key", _.key, n => c => c.copy(key = n)).addHints(HttpLabel(), Required())

  implicit val schema: Schema[TestDiscriminatedInput] = struct(
    key,
  ){
    TestDiscriminatedInput.apply
  }
  .withId(ShapeId("smithy4s.example", "TestDiscriminatedInput"))
  .addHints(
    Hints.empty
  )
}
