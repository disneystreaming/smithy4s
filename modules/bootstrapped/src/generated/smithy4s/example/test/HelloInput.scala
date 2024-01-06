package smithy4s.example.test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class HelloInput(name: String)

object HelloInput extends ShapeTag.Companion[HelloInput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "HelloInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  )

  implicit val schema: Schema[HelloInput] = struct(
    string.required[HelloInput]("name", _.name).addHints(smithy.api.HttpLabel()),
  ){
    HelloInput.apply
  }.withId(id).addHints(hints)
}
