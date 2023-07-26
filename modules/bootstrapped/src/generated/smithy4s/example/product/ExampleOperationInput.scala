package smithy4s.example.product

import smithy.api.Input
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ExampleOperationInput(a: String)
object ExampleOperationInput extends ShapeTag.Companion[ExampleOperationInput] {

  val a = string.required[ExampleOperationInput]("a", _.a, n => c => c.copy(a = n)).addHints(Required())

  implicit val schema: Schema[ExampleOperationInput] = struct(
    a,
  ){
    ExampleOperationInput.apply
  }
  .withId(ShapeId("smithy4s.example.product", "ExampleOperationInput"))
  .addHints(
    Hints(
      Input(),
    )
  )
}
