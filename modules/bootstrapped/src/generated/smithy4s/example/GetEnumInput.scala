package smithy4s.example

import smithy.api.HttpLabel
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class GetEnumInput(aa: TheEnum)
object GetEnumInput extends ShapeTag.Companion[GetEnumInput] {

  val aa: FieldLens[GetEnumInput, TheEnum] = TheEnum.schema.required[GetEnumInput]("aa", _.aa, n => c => c.copy(aa = n)).addHints(HttpLabel(), Required())

  implicit val schema: Schema[GetEnumInput] = struct(
    aa,
  ){
    GetEnumInput.apply
  }
  .withId(ShapeId("smithy4s.example", "GetEnumInput"))
}
