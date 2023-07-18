package smithy4s.example.greet

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GreetOutput(message: String)
object GreetOutput extends ShapeTag.Companion[GreetOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.greet", "GreetOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  object Lenses {
    val message = Lens[GreetOutput, String](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[GreetOutput] = struct(
    string.required[GreetOutput]("message", _.message).addHints(smithy.api.Required()),
  ){
    GreetOutput.apply
  }.withId(id).addHints(hints)
}
