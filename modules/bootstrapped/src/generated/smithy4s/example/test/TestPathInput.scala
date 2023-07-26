package smithy4s.example.test

import smithy.api.HttpLabel
import smithy.api.Input
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class TestPathInput(path: String)
object TestPathInput extends ShapeTag.Companion[TestPathInput] {

  val path: FieldLens[TestPathInput, String] = string.required[TestPathInput]("path", _.path, n => c => c.copy(path = n)).addHints(HttpLabel(), Required())

  implicit val schema: Schema[TestPathInput] = struct(
    path,
  ){
    TestPathInput.apply
  }
  .withId(ShapeId("smithy4s.example.test", "TestPathInput"))
  .addHints(
    Input(),
  )
}
