package smithy4s.example.product

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

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
