package smithy4s.example.greet

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GreetInput(name: String)

object GreetInput extends ShapeTag.Companion[GreetInput] {
  val id: ShapeId = ShapeId("smithy4s.example.greet", "GreetInput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "input"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(name: String): GreetInput = GreetInput(name)

  implicit val schema: Schema[GreetInput] = struct(
    string.required[GreetInput]("name", _.name),
  )(make).withId(id).addHints(hints)
}
