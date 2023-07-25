package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetIntEnumInput(aa: EnumResult)
object GetIntEnumInput extends ShapeTag.Companion[GetIntEnumInput] {
  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  val aa = EnumResult.schema.required[GetIntEnumInput]("aa", _.aa).addHints(smithy.api.HttpLabel(), smithy.api.Required())

  implicit val schema: Schema[GetIntEnumInput] = struct(
    aa,
  ){
    GetIntEnumInput.apply
  }.withId(ShapeId("smithy4s.example", "GetIntEnumInput")).addHints(hints)
}
