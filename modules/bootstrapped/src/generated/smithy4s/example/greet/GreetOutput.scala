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

  val hints: Hints = Hints.lazily(
    Hints(
      smithy.api.Output(),
    )
  )

  implicit val schema: Schema[GreetOutput] = struct(
    string.required[GreetOutput]("message", _.message),
  ){
    GreetOutput.apply
  }.withId(id).addHints(hints)
}
