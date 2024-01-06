package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.int

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
