package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Greeting(message: String)

object Greeting extends ShapeTag.Companion[Greeting] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "Greeting")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(message: String): Greeting = Greeting(message)

  implicit val schema: Schema[Greeting] = struct(
    string.required[Greeting]("message", _.message),
  ){
    make
  }.withId(id).addHints(hints)
}
