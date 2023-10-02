package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

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
