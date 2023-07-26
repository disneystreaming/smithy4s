package smithy4s.example.product

import smithy.api.Output
import smithy.api.Required
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ExampleOperationOutput(b: String)
object ExampleOperationOutput extends ShapeTag.Companion[ExampleOperationOutput] {

  val b: FieldLens[ExampleOperationOutput, String] = string.required[ExampleOperationOutput]("b", _.b, n => c => c.copy(b = n)).addHints(Required())

  implicit val schema: Schema[ExampleOperationOutput] = struct(
    b,
  ){
    ExampleOperationOutput.apply
  }
  .withId(ShapeId("smithy4s.example.product", "ExampleOperationOutput"))
  .addHints(
    Output(),
  )
}
