package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HeadRequestOutput(test: String)

object HeadRequestOutput extends ShapeTag.Companion[HeadRequestOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "HeadRequestOutput")

  val hints: Hints = Hints.empty

  implicit val schema: Schema[HeadRequestOutput] = struct(
    string.required[HeadRequestOutput]("test", _.test).addHints(smithy.api.HttpHeader("Test")),
  ){
    HeadRequestOutput.apply
  }.withId(id).addHints(hints)
}
