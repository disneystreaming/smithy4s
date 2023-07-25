package smithy4s.example.greet

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GreetOutput(message: String)
object GreetOutput extends ShapeTag.Companion[GreetOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.greet", "GreetOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  val message = string.required[GreetOutput]("message", _.message).addHints(smithy.api.Required())

  implicit val schema: Schema[GreetOutput] = struct(
    message,
  ){
    GreetOutput.apply
  }.withId(id).addHints(hints)
}
