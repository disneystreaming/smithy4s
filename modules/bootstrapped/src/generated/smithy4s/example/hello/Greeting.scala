package smithy4s.example.hello

import _root_.smithy4s.Hints
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.ShapeTag
import _root_.smithy4s.schema.Schema.struct
import smithy4s.schema.Schema.string

final case class Greeting(message: String)

object Greeting extends ShapeTag.Companion[Greeting] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "Greeting")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[Greeting] = struct(
    string.required[Greeting]("message", _.message),
  ){
    Greeting.apply
  }.withId(id).addHints(hints)
}
