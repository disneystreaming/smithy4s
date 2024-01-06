package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct

final case class GetIntEnumInput(aa: EnumResult)

object GetIntEnumInput extends ShapeTag.Companion[GetIntEnumInput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetIntEnumInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[GetIntEnumInput] = struct(
    EnumResult.schema.required[GetIntEnumInput]("aa", _.aa).addHints(smithy.api.HttpLabel()),
  ){
    GetIntEnumInput.apply
  }.withId(id).addHints(hints)
}
