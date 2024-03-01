package smithy4s.example.test

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HelloOutput(message: String)

object HelloOutput extends ShapeTag.Companion[HelloOutput] {
  val id: ShapeId = ShapeId("smithy4s.example.test", "HelloOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: String): HelloOutput = HelloOutput(message)

  implicit val schema: Schema[HelloOutput] = struct(
    string.required[HelloOutput]("message", _.message),
  ){
    make
  }.withId(id).addHints(hints)
}
