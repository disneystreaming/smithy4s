package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class CustomCodeInput(code: Int)
object CustomCodeInput extends ShapeTag.Companion[CustomCodeInput] {

  val code: FieldLens[CustomCodeInput, Int] = int.required[CustomCodeInput]("code", _.code, n => c => c.copy(code = n)).addHints(HttpLabel(), Required())

  implicit val schema: Schema[CustomCodeInput] = struct(
    code,
  ){
    CustomCodeInput.apply
  }
  .withId(ShapeId("smithy4s.example", "CustomCodeInput"))
}
