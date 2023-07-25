package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HelloInput(name: String)
object HelloInput extends ShapeTag.Companion[HelloInput] {
  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  val name = string.required[HelloInput]("name", _.name).addHints(smithy.api.HttpLabel(), smithy.api.Required())

  implicit val schema: Schema[HelloInput] = struct(
    name,
  ){
    HelloInput.apply
  }.withId(ShapeId("smithy4s.example.test", "HelloInput")).addHints(hints)
}
