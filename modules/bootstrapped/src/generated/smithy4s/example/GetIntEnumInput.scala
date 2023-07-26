package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.Input
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class GetIntEnumInput(aa: EnumResult)
object GetIntEnumInput extends ShapeTag.$Companion[GetIntEnumInput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "GetIntEnumInput")

  val $hints: Hints = Hints(
    Input(),
  )

  val aa: FieldLens[GetIntEnumInput, EnumResult] = EnumResult.$schema.required[GetIntEnumInput]("aa", _.aa, n => c => c.copy(aa = n)).addHints(HttpLabel(), Required())

  implicit val $schema: Schema[GetIntEnumInput] = struct(
    aa,
  ){
    GetIntEnumInput.apply
  }.withId($id).addHints($hints)
}
