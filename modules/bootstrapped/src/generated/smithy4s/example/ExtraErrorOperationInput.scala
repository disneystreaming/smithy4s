package smithy4s.example

import smithy.api.Input
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ExtraErrorOperationInput(in: Option[String] = None)
object ExtraErrorOperationInput extends ShapeTag.Companion[ExtraErrorOperationInput] {

  val in: FieldLens[ExtraErrorOperationInput, Option[String]] = string.optional[ExtraErrorOperationInput]("in", _.in, n => c => c.copy(in = n))

  implicit val schema: Schema[ExtraErrorOperationInput] = struct(
    in,
  ){
    ExtraErrorOperationInput.apply
  }
  .withId(ShapeId("smithy4s.example", "ExtraErrorOperationInput"))
  .addHints(
    Input(),
  )
}
