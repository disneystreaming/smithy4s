package smithy4s.example

import smithy.api.Output
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.struct

final case class GetIntEnumOutput(result: EnumResult)
object GetIntEnumOutput extends ShapeTag.$Companion[GetIntEnumOutput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "GetIntEnumOutput")

  val $hints: Hints = Hints(
    Output(),
  )

  val result: FieldLens[GetIntEnumOutput, EnumResult] = EnumResult.$schema.required[GetIntEnumOutput]("result", _.result, n => c => c.copy(result = n)).addHints(Required())

  implicit val $schema: Schema[GetIntEnumOutput] = struct(
    result,
  ){
    GetIntEnumOutput.apply
  }.withId($id).addHints($hints)
}
