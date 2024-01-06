package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class GetEnumInput(aa: TheEnum)

object GetEnumInput extends ShapeTag.Companion[GetEnumInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetEnumInput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[GetEnumInput] = struct(
    TheEnum.schema.required[GetEnumInput]("aa", _.aa).addHints(smithy.api.HttpLabel()),
  ){
    GetEnumInput.apply
  }.withId(id).addHints(hints)
}
