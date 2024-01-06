package smithy4s.example.greet

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GreetInput(name: String)

object GreetInput extends ShapeTag.Companion[GreetInput] {
  val id: ShapeId = ShapeId("smithy4s.example.greet", "GreetInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[GreetInput] = struct(
    string.required[GreetInput]("name", _.name),
  ){
    GreetInput.apply
  }.withId(id).addHints(hints)
}
