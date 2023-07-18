package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.optics.Lens
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HelloOutput(message: String)
object HelloOutput extends ShapeTag.Companion[HelloOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "HelloOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  )

  object Lenses {
    val message = Lens[HelloOutput, String](_.message)(n => a => a.copy(message = n))
  }

  implicit val schema: Schema[HelloOutput] = struct(
    string.required[HelloOutput]("message", _.message).addHints(smithy.api.Required()),
  ){
    HelloOutput.apply
  }.withId(id).addHints(hints)
}
