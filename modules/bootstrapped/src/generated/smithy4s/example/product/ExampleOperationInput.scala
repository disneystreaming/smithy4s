package smithy4s.example.product

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class ExampleOperationInput(a: String)
object ExampleOperationInput extends ShapeTag.Companion[ExampleOperationInput] {
  val id: ShapeId = ShapeId("smithy4s.example.product", "ExampleOperationInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  object Optics {
    val a = Lens[ExampleOperationInput, String](_.a)(n => a => a.copy(a = n))
  }

  implicit val schema: Schema[ExampleOperationInput] = struct(
    string.required[ExampleOperationInput]("a", _.a).addHints(smithy.api.Required()),
  ){
    ExampleOperationInput.apply
  }.withId(id).addHints(hints)
}
