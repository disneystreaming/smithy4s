package smithy4s.example.test

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class HelloOutput(message: String)

object HelloOutput extends ShapeTag.Companion[HelloOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "HelloOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  implicit val schema: Schema[HelloOutput] = struct(
    string.required[HelloOutput]("message", _.message),
  ){
    HelloOutput.apply
  }.withId(id).addHints(hints)
}
