package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Greeting(message: String)
object Greeting extends ShapeTag.Companion[Greeting] {
  val hints: Hints = Hints.empty

  val message = string.required[Greeting]("message", _.message).addHints(smithy.api.Required())

  implicit val schema: Schema[Greeting] = struct(
    message,
  ){
    Greeting.apply
  }.withId(ShapeId("smithy4s.example.hello", "Greeting")).addHints(hints)
}
