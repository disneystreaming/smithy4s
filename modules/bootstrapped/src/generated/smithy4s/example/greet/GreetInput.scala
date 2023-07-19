package smithy4s.example.greet

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GreetInput(name: String)
object GreetInput extends ShapeTag.Companion[GreetInput] {
  val id: ShapeId = ShapeId("smithy4s.example.greet", "GreetInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  object Optics {
    val name = Lens[GreetInput, String](_.name)(n => a => a.copy(name = n))
  }

  implicit val schema: Schema[GreetInput] = struct(
    string.required[GreetInput]("name", _.name).addHints(smithy.api.Required()),
  ){
    GreetInput.apply
  }.withId(id).addHints(hints)
}
