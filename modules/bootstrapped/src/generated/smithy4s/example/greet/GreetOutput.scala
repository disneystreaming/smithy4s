package smithy4s.example.greet

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class GreetOutput(message: String)

object GreetOutput extends ShapeTag.Companion[GreetOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.greet", "GreetOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  implicit val schema: Schema[GreetOutput] = struct(
    string.required[GreetOutput]("message", _.message),
  ){
    GreetOutput.apply
  }.withId(id).addHints(hints)
}
