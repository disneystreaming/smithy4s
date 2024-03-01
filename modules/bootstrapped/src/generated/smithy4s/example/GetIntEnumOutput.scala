package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetIntEnumOutput(result: EnumResult)

object GetIntEnumOutput extends ShapeTag.Companion[GetIntEnumOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetIntEnumOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(result: EnumResult): GetIntEnumOutput = GetIntEnumOutput(result)

  implicit val schema: Schema[GetIntEnumOutput] = struct(
    EnumResult.schema.required[GetIntEnumOutput]("result", _.result),
  ){
    make
  }.withId(id).addHints(hints)
}
