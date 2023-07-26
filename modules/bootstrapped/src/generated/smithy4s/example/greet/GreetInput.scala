package smithy4s.example.greet

import smithy.api.Input
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.FieldLens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GreetInput(name: String)
object GreetInput extends ShapeTag.$Companion[GreetInput] {
  val $id: ShapeId = ShapeId("smithy4s.example.greet", "GreetInput")

  val $hints: Hints = Hints(
    Input(),
  )

  val name: FieldLens[GreetInput, String] = string.required[GreetInput]("name", _.name, n => c => c.copy(name = n)).addHints(Required())

  implicit val $schema: Schema[GreetInput] = struct(
    name,
  ){
    GreetInput.apply
  }.withId($id).addHints($hints)
}
