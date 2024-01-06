package smithy4s.example.product

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class ExampleOperationInput(a: String)

object ExampleOperationInput extends ShapeTag.Companion[ExampleOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example.product", "ExampleOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[ExampleOperationInput] = struct(
    string.required[ExampleOperationInput]("a", _.a),
  ){
    ExampleOperationInput.apply
  }.withId(id).addHints(hints)
}
