package smithy4s.example.product

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ExampleOperationOutput(b: String)
object ExampleOperationOutput extends ShapeTag.Companion[ExampleOperationOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.product", "ExampleOperationOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  implicit val schema: Schema[ExampleOperationOutput] = struct(
    string.required[ExampleOperationOutput]("b", _.b),
  ){
    ExampleOperationOutput.apply
  }.withId(id).addHints(hints)
}
