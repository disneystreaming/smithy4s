package smithy4s.example.product

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ExampleOperationOutput(b: String)
object ExampleOperationOutput extends ShapeTag.Companion[ExampleOperationOutput] {
  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  val b = string.required[ExampleOperationOutput]("b", _.b).addHints(smithy.api.Required())

  implicit val schema: Schema[ExampleOperationOutput] = struct(
    b,
  ){
    ExampleOperationOutput.apply
  }.withId(ShapeId("smithy4s.example.product", "ExampleOperationOutput")).addHints(hints)
}
