package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class CustomCodeInput(code: Int)
object CustomCodeInput extends ShapeTag.Companion[CustomCodeInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "CustomCodeInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[CustomCodeInput] = struct(
    int.required[CustomCodeInput]("code", _.code).addHints(smithy.api.HttpLabel()),
  ){
    CustomCodeInput.apply
  }.withId(id).addHints(hints)
}
