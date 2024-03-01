package smithy4s.example.product

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ExampleOperationInput(a: String)

object ExampleOperationInput extends ShapeTag.Companion[ExampleOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example.product", "ExampleOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(a: String): ExampleOperationInput = ExampleOperationInput(a)

  implicit val schema: Schema[ExampleOperationInput] = struct(
    string.required[ExampleOperationInput]("a", _.a),
  ){
    make
  }.withId(id).addHints(hints)
}
