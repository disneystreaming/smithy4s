package smithy4s.example

import smithy.api.HttpResponseCode
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class CustomCodeOutput(code: Option[Int] = None)
object CustomCodeOutput extends ShapeTag.$Companion[CustomCodeOutput] {
  val $id: ShapeId = ShapeId("smithy4s.example", "CustomCodeOutput")

  val $hints: Hints = Hints.empty

  val code: FieldLens[CustomCodeOutput, Option[Int]] = int.optional[CustomCodeOutput]("code", _.code, n => c => c.copy(code = n)).addHints(HttpResponseCode())

  implicit val $schema: Schema[CustomCodeOutput] = struct(
    code,
  ){
    CustomCodeOutput.apply
  }.withId($id).addHints($hints)
}
