package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GetEnumOutput(result: Option[String] = None)
object GetEnumOutput extends ShapeTag.$Companion[GetEnumOutput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "GetEnumOutput")

  val $hints: Hints = Hints.empty

  val result: FieldLens[GetEnumOutput, Option[String]] = string.optional[GetEnumOutput]("result", _.result, n => c => c.copy(result = n))

  implicit val $schema: Schema[GetEnumOutput] = struct(
    result,
  ){
    GetEnumOutput.apply
  }.withId($id).addHints($hints)
}
